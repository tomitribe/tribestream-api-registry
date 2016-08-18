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
package com.tomitribe.tribestream.registryng.resources;

import com.tomitribe.tribestream.registryng.security.PrincipalDto;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.openejb.loader.SystemInstance;
import org.apache.openejb.spi.SecurityService;
import org.apache.openejb.util.reflection.Reflections;
import org.apache.tomee.catalina.TomcatSecurityService;

import javax.annotation.PostConstruct;
import javax.annotation.security.RolesAllowed;
import javax.ejb.Lock;
import javax.ejb.LockType;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Singleton
@Lock(LockType.READ)
@TransactionAttribute(TransactionAttributeType.SUPPORTS)
@Path("login")
@Consumes("application/json")
@Produces("application/json")
public class LoginResource {

    @Context
    private HttpServletRequest request;

    @Context
    private SecurityContext securityContext;

    private SecurityService securityService;

    @PostConstruct
    public void init() {
        this.securityService = SystemInstance.get().getComponent(SecurityService.class);
    }

    @POST
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED, MediaType.MULTIPART_FORM_DATA})
    public Response authenticate(@FormParam("username") final String username,
                                 @FormParam("password") final String password) {

        if (username == null || username.trim().isEmpty()) {
            throw new NullPointerException("`username` is required.");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new NullPointerException("`password` is required.");
        }

        try {
            request.login(username, password);

            return Response.ok(principalToDto(tomcatPrincipal())).build();

        } catch (NullPointerException e) {
            return Response.status(400).build();
        } catch (final Exception e) {
            throw new WebApplicationException(e, Response.Status.UNAUTHORIZED);

        } finally {
            try {
                request.logout();
            } catch (final ServletException e) {
                // no-op
            }
        }
    }

    @GET
    @RolesAllowed("tribe-console") // default console role - might not be relevant to enforce a role
    public PrincipalDto getAuthenticatedPrincipal() {
        return principalToDto(request.getUserPrincipal());
    }

    private PrincipalDto principalToDto(final Principal userPrincipal) {
        Subject subject = null;
        final Map<String, String> userAttributes = new HashMap<>(10);

        // with tomcat by default, including our FastJaasRealm
        if (GenericPrincipal.class.isInstance(userPrincipal)) {
            final GenericPrincipal principal = GenericPrincipal.class.cast(userPrincipal);
            final Principal up = principal.getUserPrincipal();

            // subject isn't accessible
            if (subject == null) {
                final LoginContext loginContext = (LoginContext) Reflections.get(principal, "loginContext");
                subject = null != loginContext ? loginContext.getSubject() : null;
            }
        }

//        if (subject != null) {
//            final Set<UserAttributePrincipal> userAttr = subject.getPrincipals(UserAttributePrincipal.class);
//            for (final UserAttributePrincipal userAttributePrincipal : userAttr) {
//                userAttributes.put(userAttributePrincipal.getName(), userAttributePrincipal.getValue());
//            }
//        }

        return new PrincipalDto(userPrincipal.getName(), roles(userPrincipal), userAttributes);
    }

    private Principal tomcatPrincipal() {
        final Principal userPrincipal = securityService.getCallerPrincipal();
        if (TomcatSecurityService.TomcatUser.class.isInstance(userPrincipal)) {
            return TomcatSecurityService.TomcatUser.class.cast(userPrincipal).getTomcatPrincipal();
        }
        return userPrincipal;
    }

    private String[] roles(final Principal userPrincipal) {
        Principal pcp = userPrincipal;
        if (!GenericPrincipal.class.isInstance(userPrincipal)) {
            pcp = tomcatPrincipal();
        }
        return GenericPrincipal.class.isInstance(pcp) ? GenericPrincipal.class.cast(pcp).getRoles() : null;
    }
}
