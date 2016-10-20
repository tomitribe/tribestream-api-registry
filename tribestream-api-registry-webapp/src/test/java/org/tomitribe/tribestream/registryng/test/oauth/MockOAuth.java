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
package org.tomitribe.tribestream.registryng.test.oauth;

import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.IntrospectResponse;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Path("oauth2")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_FORM_URLENCODED)
@Produces(MediaType.APPLICATION_JSON)
public class MockOAuth {
    private static final String ACCESS_TOKEN = "foo." + Base64.getUrlEncoder().encodeToString("valid".getBytes(StandardCharsets.UTF_8)) + ".bar";
    private static final String USERNAME = "useroauth2";

    @POST
    @Path("token")
    public AccessTokenResponse token(final MultivaluedHashMap<String, String> form) {
        if (USERNAME.equals(form.getFirst("username"))) {
            final AccessTokenResponse token = new AccessTokenResponse();
            token.setExpiresIn(3600);
            token.setTokenType("bearer");
            token.setRefreshToken("refresh");
            token.setAccessToken(ACCESS_TOKEN);
            return token;
        }
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }

    @POST
    @Path("introspect")
    public IntrospectResponse introspect(final MultivaluedHashMap<String, String> form) {
        if (ACCESS_TOKEN.equals(form.getFirst("token"))) {
            final IntrospectResponse token = new IntrospectResponse();
            token.setExpiresIn(3600);
            token.setTokenType("bearer");
            token.setActive(true);
            token.setUsername(USERNAME);
            return token;
        }
        throw new WebApplicationException(Response.Status.BAD_REQUEST);
    }
}
