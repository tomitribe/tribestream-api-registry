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
package org.tomitribe.tribestream.registryng.resources;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenService;
import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
import java.util.logging.Level;
import java.util.logging.Logger;

@ApplicationScoped
@Path("security/oauth2")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public class Oauth2TokenResource {

    private static final Logger LOG = Logger.getLogger(Oauth2TokenResource.class.getName());

    @Inject
    @ConfigProperty(name = "registry.oauth2.gatewayUrl")
    private String tagUrl;

    @Inject
    @ConfigProperty(name = "registry.oauth2.clientId")
    private String clientId;

    @Inject
    @ConfigProperty(name = "registry.oauth2.clientSecret")
    private String clientSecret;

    @Inject
    @ConfigProperty(name = "registry.oauth2.tlsProtocol", defaultValue = "TLSv1.2")
    private String tlsProtocol;

    @Inject
    @ConfigProperty(name = "registry.oauth2.tlsProvider")
    private String tlsProvider;

    @Inject
    @ConfigProperty(name = "registry.oauth2.trustStore")
    private String trustStoreFileName;

    @Inject
    @ConfigProperty(name = "registry.oauth2.trustStoreType")
    private String trustStoreType;

    @Inject
    @ConfigProperty(name = "registry.oauth2.trustStoreProvider")
    private String trustStoreProvider;

    @Inject
    private AccessTokenService accessTokenService;

    private volatile SSLContext sslContext;

    @POST
    public Response getToken(final MultivaluedMap<String, String> formParameters) {

        if (tagUrl == null || tagUrl.trim().length() == 0) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity("No Oauth2 gateway configured")
                    .build();
        }
        // TODO: Configure, set SSL Truststore or SSLContext etc.
        // TODO: Pool clients
        Client client = null;
        try {
            client = ClientBuilder.newBuilder()
                    .sslContext(getSslContext())
                    .build();
            if (clientId != null) {
                client.register(new BasicAuthFilter());
            }
            client.register(new CustomJacksonJaxbJsonProvider());
            WebTarget target = client.target(tagUrl);

            // Pass the client parameters through
            Form form = new Form(formParameters);

            Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

            if (response.getStatus() == Response.Status.OK.getStatusCode()) {
                final AccessTokenResponse responsePayload = response.readEntity(AccessTokenResponse.class);
                accessTokenService.addAccessToken(responsePayload);
                return Response.status(response.getStatus())
                        .entity(responsePayload)
                        .build();
            } else {
                final String responsePayload = response.readEntity(String.class);
                return Response.status(response.getStatus())
                        .entity(responsePayload)
                        .build();
            }
        } catch (GeneralSecurityException e) {
            LOG.log(Level.SEVERE, "Cannot setup Oauth2 client!", e);
            return Response.serverError().build();
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    private SSLContext getSslContext() throws GeneralSecurityException {
        if (sslContext == null) {
            SSLContext newSslContext;
            if (tlsProvider != null) {
                newSslContext = SSLContext.getInstance(tlsProtocol, tlsProvider);
            } else {
                newSslContext = SSLContext.getInstance(tlsProtocol);
            }


            final KeyStore trustStore;
            if (trustStoreFileName != null) {
                if (trustStoreType == null) {
                    if (trustStoreProvider == null) {
                        trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                    } else {
                        trustStore = KeyStore.getInstance(KeyStore.getDefaultType(), trustStoreProvider);
                    }
                } else {
                    if (trustStoreProvider == null) {
                        trustStore = KeyStore.getInstance(trustStoreType);
                    } else {
                        trustStore = KeyStore.getInstance(trustStoreType, trustStoreProvider);
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
            sslContext = newSslContext;
        }
        return sslContext;
    }

    private class BasicAuthFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext clientRequestContext) throws IOException {
            final byte[] plain = (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8);
            final String encoded = Base64.getEncoder().encodeToString(plain);
            clientRequestContext.getHeaders().putSingle("Authorization", "Basic " + encoded);
        }
    }

}
