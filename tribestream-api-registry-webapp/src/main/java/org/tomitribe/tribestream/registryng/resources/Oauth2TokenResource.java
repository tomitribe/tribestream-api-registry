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

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
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
import java.util.Base64;

@ApplicationScoped
@Path("security/oauth2")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public class Oauth2TokenResource {

    private static final String GRANT_TYPE = "grant_type";

    private static final String GRANT_TYPE_PASSWORD = "password";

    private static final String GRANT_TYPE_REFRESH_TOKEN = "refresh_token";

    @Inject
    @ConfigProperty(name = "registry.oauthGatewayUrl")
    private String tagUrl;

    @Inject
    @ConfigProperty(name = "registry.clientId")
    private String clientId;

    @Inject
    @ConfigProperty(name = "registry.clientSecret")
    private String clientSecret;

    @Inject
    private AccessTokenService accessTokenService;

    @POST
    public Response getToken(final MultivaluedMap<String, String> formParameters) {

        // TODO: Configure, set SSL Truststore or SSLContext etc.
        // TODO: Pool clients
        Client client = null;
        try {
            client = ClientBuilder.newClient();
            if (clientId != null) {
                client.register(new BasicAuthFilter());
            }
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

        } finally {
            client.close();
        }
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
