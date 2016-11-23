/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0

 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tomitribe.tribestream.registryng.service.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.auth.signatures.Signature;
import org.tomitribe.auth.signatures.Signer;
import org.tomitribe.tribestream.registryng.documentation.Description;
import org.tomitribe.util.Duration;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toMap;

// here we dont reuse client instances to avoid to handle thread safety since it is not a high throughput service
@ApplicationScoped
public class GenericClientService {
    @Inject
    @Description("Timeout for the request done with the `try me` feature.")
    @ConfigProperty(name = "tribe.registry.ui.try-me.timeout", defaultValue = "30 seconds")
    private String timeoutLiteral;

    @Inject
    @Description("Default endpoint used for oauth2 request when not specified in the UI.")
    @ConfigProperty(name = "tribe.registry.ui.try-me.oauth2.default-endpoint")
    private String oauth2Endpoint;

    @Inject
    @Description("Default OAuth2 client used.")
    @ConfigProperty(name = "tribe.registry.ui.try-me.oauth2.client.name")
    private String oauth2Client;

    @Inject
    @Description("Default OAuth2 client secret used (only with `tribe.registry.ui.try-me.oauth2.client.name`).")
    @ConfigProperty(name = "tribe.registry.ui.try-me.oauth2.client.secret")
    private String oauth2ClientSecret;

    private String timeout;

    @PostConstruct
    private void init() {
        timeout = String.valueOf(new Duration(timeoutLiteral, TimeUnit.MILLISECONDS).getTime(TimeUnit.MILLISECONDS));
    }

    public String oauth2Header(final String grantType,
                               final String username,
                               final String password,
                               final String refreshToken,
                               final String clientId,
                               final String clientSecret,
                               final String endpoint,
                               final boolean ignoreSsl) {
        final Client client = newClient(ignoreSsl && endpoint != null && endpoint.startsWith("https"));
        try {
            final Form form = new Form();
            switch (ofNullable(grantType).map(g -> g.toLowerCase(Locale.ENGLISH)).orElse("password")) {
                case "password":
                    form.param("username", username).param("password", password).param("grant_type", "password");
                    break;
                case "refresh_token":
                    form.param("refresh_token", refreshToken).param("grant_type", "refreshToken");
                    break;
                default:
                    throw new IllegalArgumentException("Unsupported oauth2 grant_type: " + grantType);
            }

            final String clientName = ofNullable(clientId).orElse(oauth2Client);
            ofNullable(clientName)
                    .ifPresent(c -> form.param("client_id", c));
            ofNullable(oauth2Client != null && oauth2Client.equals(clientName) && clientSecret == null ? oauth2ClientSecret : clientSecret)
                    .ifPresent(c -> form.param("client_secret", c));

            final Token token = client
                    .target(ofNullable(endpoint).orElse(oauth2Endpoint))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE), Token.class);
            return token.getToken_type() + " " + token.getAccess_token();
        } finally {
            client.close();
        }
    }

    public String httpSign(final List<String> headers,
                           final String method,
                           final String path,
                           final String alias,
                           final String secret,
                           final String algorithm,
                           final Map<String, String> requestHeaders) {
        try {
            final Signer signer = new Signer(
                    new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), algorithm),
                    new Signature(alias, algorithm, null, headers));

            headers.forEach(h -> { // some particular and common headers to ensure we can handle autoamtically
                switch (h.toLowerCase(Locale.ENGLISH)) {
                    case "date": {
                        if (!requestHeaders.keySet().stream().filter(s -> s.equalsIgnoreCase("date")).findAny().isPresent()) {
                            requestHeaders.put("date", new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US) {{
                                setTimeZone(TimeZone.getTimeZone("GMT"));
                            }}.format(new Date()));
                        }
                        break;
                    }
                    case "(request-target)": // virtual header handled in Signer
                        break;
                    default:
                        if (!requestHeaders.containsKey(h)) {
                            throw new IllegalArgumentException(h + " header not yet supported");
                        }
                }
            });

            return signer.sign(method, path, requestHeaders).toString();
        } catch (final IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }

    public String basicHeader(final String username, final String password) {
        return "Basic " + Base64.getEncoder().encodeToString(
                Stream.of(username, password).collect(joining(":")).getBytes(StandardCharsets.UTF_8));
    }

    public String digestHeader(final String payload, final String algorithm) {
        final MessageDigest digest;
        try {
            digest = MessageDigest.getInstance(algorithm);
        } catch (final NoSuchAlgorithmException e) {
            throw new IllegalArgumentException(e);
        }
        digest.update(payload.getBytes(StandardCharsets.UTF_8));
        return algorithm + '=' + Base64.getEncoder().encodeToString(digest.digest());
    }

    public Response invoke(final Request request) {
        final Client client = newClient(request.isIgnoreSsl() && request.getUrl() != null && request.getUrl().startsWith("https"))
                .property("http.connection.timeout", timeout)
                .property("http.receive.timeout", timeout)
                .property("jersey.config.client.connectTimeout", timeout)
                .property("jersey.config.client.readTimeout", timeout);

        final javax.ws.rs.core.Response response;
        try {
            final Map<String, String> headers = new HashMap<>(ofNullable(request.getHeaders()).orElse(emptyMap()));
            final Invocation.Builder builder = client.target(request.getUrl()).request(headers.getOrDefault("Accept", MediaType.WILDCARD));
            headers.forEach(builder::header);

            final String payload = request.getPayload();
            if (payload == null || payload.isEmpty()) {
                response = builder.method(request.method);
            } else {
                response = builder.method(request.method, Entity.entity(
                        payload,
                        headers.computeIfAbsent("Content-Type", k -> payload.startsWith("{") ?
                                MediaType.APPLICATION_JSON : (payload.startsWith("<") ? MediaType.APPLICATION_XML : MediaType.WILDCARD))));
            }
            return new Response(
                    response.getStatus(),
                    response.getStringHeaders().entrySet().stream()
                            .collect(toMap(
                                    Map.Entry::getKey,
                                    t -> t.getValue().stream().collect(joining(",")),
                                    (s, s2) -> Stream.of(s, s2).filter(v -> v != null).collect(joining(",")))),
                    response.getStatus() != javax.ws.rs.core.Response.Status.NO_CONTENT.getStatusCode() ? response.readEntity(String.class) : "");
        } finally {
            client.close();
        }
    }

    private Client newClient(final boolean ignoreSsl) {
        final ClientBuilder builder = ClientBuilder.newBuilder();
        if (ignoreSsl) {
            builder.sslContext(getSSLContext()).hostnameVerifier((s, session) -> true);
        }
        return builder.build();
    }

    private SSLContext getSSLContext() {
        try {
            final SSLContext sslContext = SSLContext.getInstance("SSL");
            sslContext.init(null, new TrustManager[]{new X509TrustManager() {
                @Override
                public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                    // no-op
                }

                @Override
                public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                    // no-op
                }

                @Override
                public X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

            }}, new SecureRandom());
            return sslContext;
        } catch (final NoSuchAlgorithmException | KeyManagementException e) {
            throw new IllegalStateException(e);
        }
    }

    @Data
    public static class Request {
        private boolean ignoreSsl;
        private String method;
        private String url;
        private Map<String, String> headers;
        private String payload;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Response {
        private int status;
        private Map<String, String> headers;
        private String payload;
    }

    @Data
    public static class Token {
        private String access_token;
        private String token_type;
    }
}
