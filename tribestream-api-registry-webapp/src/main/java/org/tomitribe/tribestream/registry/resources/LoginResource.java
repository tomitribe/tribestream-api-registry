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
package org.tomitribe.tribestream.registry.resources;

import org.tomitribe.tribestream.registry.security.PrincipalDto;

import javax.annotation.security.RolesAllowed;
import javax.enterprise.context.ApplicationScoped;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.logging.Level;
import java.util.logging.Logger;

@Path("login")
@ApplicationScoped
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class LoginResource {
    private static final Logger LOGGER = Logger.getLogger(LoginResource.class.getName());

    @Context
    private SecurityContext securityContext;

    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
    public Response authenticate(@FormParam("username") final String username,
                                 @FormParam("password") final String password,
                                 @Context final HttpServletRequest request) {

        if (username == null || username.trim().isEmpty()) {
            throw new NullPointerException("`username` is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new NullPointerException("`password` is required.");
        }

        try {
            request.login(username, password);
            LOGGER.log(Level.INFO, () -> String.format("Successful login of user '%s'", username));

            return Response.ok(principalToDto(request.getUserPrincipal())).build();

        } catch (final ServletException e) {
            LOGGER.log(Level.SEVERE, e, () -> String.format("Login failed for user %s", username));
            return Response.status(Response.Status.UNAUTHORIZED).build();
        } finally {
            try {
                request.logout();
            } catch (final ServletException e) {
                LOGGER.log(Level.WARNING, e, () -> String.format("Unexpected exception during login of user %s", username));
            }
        }
    }

    @GET
    @RolesAllowed("tribe-console") // default console role - might not be relevant to enforce a role
    public PrincipalDto getAuthenticatedPrincipal(@Context final HttpServletRequest request) {
        return principalToDto(request.getUserPrincipal());
    }

    private PrincipalDto principalToDto(final Principal userPrincipal) {
        return new PrincipalDto(userPrincipal.getName());
    }
}
