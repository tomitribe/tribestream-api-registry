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
package org.tomitribe.tribestream.registryng.security.oauth2;

import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Base64;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class OAuth2Client {
    @Inject
    private Oauth2Configuration oauth2Config;

    private Client client;

    public Response introspect(final String accessToken) {
        if (oauth2Config.getIntrospectServerUrl() == null || oauth2Config.getIntrospectServerUrl().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No Oauth2 /introspect gateway configured")
                    .build();
        }

        final WebTarget target = client.target(oauth2Config.getIntrospectServerUrl());
        final Form form = new Form().param("token", accessToken);
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    public Response token(final MultivaluedMap<String, String> formParameters) {
        if (oauth2Config.getAuthServerUrl() == null || oauth2Config.getAuthServerUrl().isEmpty()) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No Oauth2 gateway configured")
                    .build();
        }

        final WebTarget target = client.target(oauth2Config.getAuthServerUrl());
        final Form form = new Form(formParameters);
        return target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));
    }

    @PostConstruct
    private void load() {
        try {
            client = ClientBuilder.newBuilder()
                    .sslContext(createSslContext())
                    .build();
            if (oauth2Config.getClientId() != null && !oauth2Config.getClientId().isEmpty()) {
                client.register(new BasicAuthFilter());
            }
            client.register(new CustomJacksonJaxbJsonProvider());
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    private void destroy() {
        ofNullable(client).ifPresent(Client::close);
    }

    private SSLContext createSslContext() throws GeneralSecurityException {
        SSLContext newSslContext;
        if (oauth2Config.getTlsProvider() != null) {
            newSslContext = SSLContext.getInstance(oauth2Config.getTlsProtocol(), oauth2Config.getTlsProvider());
        } else {
            newSslContext = SSLContext.getInstance(oauth2Config.getTlsProtocol());
        }


        final KeyStore trustStore;
        if (oauth2Config.getTrustStoreFileName() != null && oauth2Config.getTrustStoreFileName().length() > 0) {
            if (oauth2Config.getTrustStoreType() == null) {
                if (oauth2Config.getTlsProvider() == null) {
                    trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                } else {
                    trustStore = KeyStore.getInstance(KeyStore.getDefaultType(), oauth2Config.getTlsProvider());
                }
            } else {
                if (oauth2Config.getTlsProvider() == null) {
                    trustStore = KeyStore.getInstance(oauth2Config.getTrustStoreType());
                } else {
                    trustStore = KeyStore.getInstance(oauth2Config.getTrustStoreType(), oauth2Config.getTlsProvider());
                }
            }
        } else {
            trustStore = null;
        }

        final TrustManagerFactory tmfactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmfactory.init(trustStore);

        final TrustManager[] tms = tmfactory.getTrustManagers();
        if (tms != null) {
            for (int i = 0; i < tms.length; ++i) {
                final TrustManager mgr = tms[i];
                if (!X509TrustManager.class.isInstance(mgr)) {
                    continue;
                }

                final X509TrustManager x509TrustManager = X509TrustManager.class.cast(mgr);
                tms[i] = new X509TrustManager() {
                    @Override
                    public void checkClientTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                        x509TrustManager.checkClientTrusted(x509Certificates, s);
                    }

                    @Override
                    public void checkServerTrusted(final X509Certificate[] x509Certificates, final String s) throws CertificateException {
                        if (x509Certificates.length == 1) {
                            x509TrustManager.checkServerTrusted(x509Certificates, s);
                        }
                    }

                    @Override
                    public X509Certificate[] getAcceptedIssuers() {
                        return x509TrustManager.getAcceptedIssuers();
                    }
                };
            }
        }
        newSslContext.init(null, tms, new SecureRandom());
        return newSslContext;
    }

    private class BasicAuthFilter implements ClientRequestFilter {
        @Override
        public void filter(final ClientRequestContext clientRequestContext) throws IOException {
            final byte[] plain = (oauth2Config.getClientId() + ":" + oauth2Config.getClientSecret()).getBytes(StandardCharsets.UTF_8);
            final String encoded = Base64.getEncoder().encodeToString(plain);
            clientRequestContext.getHeaders().putSingle("Authorization", "Basic " + encoded);
        }
    }
}
