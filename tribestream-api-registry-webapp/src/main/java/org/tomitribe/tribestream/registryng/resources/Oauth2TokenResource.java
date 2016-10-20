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

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.OAuth2Client;
import org.tomitribe.tribestream.registryng.security.oauth2.Oauth2Configuration;
import org.tomitribe.tribestream.registryng.security.oauth2.OAuth2Tokens;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@ApplicationScoped
@Path("security/oauth2")
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public class Oauth2TokenResource {
    @Inject
    private Oauth2Configuration oauth2Config;

    @Inject
    private OAuth2Tokens oauth2Tokens;

    @Inject
    private OAuth2Client client;

    @GET
    @Path("/status")
    @Produces(MediaType.APPLICATION_JSON)
    public Oauth2Status getOauth2Status() {
        return Oauth2Status.builder()
                .enabled(oauth2Config.getAuthServerUrl() != null && oauth2Config.getAuthServerUrl().length() > 0)
                .build();
    }

    @POST
    public Response getToken(final MultivaluedMap<String, String> formParameters) {
        final Response response = client.token(formParameters);
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            final AccessTokenResponse responsePayload = response.readEntity(AccessTokenResponse.class);
            oauth2Tokens.save(responsePayload);
            return Response.status(response.getStatus())
                    .entity(responsePayload)
                    .build();
        } else {
            final String responsePayload = response.readEntity(String.class);
            return Response.status(response.getStatus())
                    .entity(responsePayload)
                    .build();
        }
    }

    @Getter
    @Setter
    @Builder
    public static class Oauth2Status {
        private boolean enabled;
    }
}
