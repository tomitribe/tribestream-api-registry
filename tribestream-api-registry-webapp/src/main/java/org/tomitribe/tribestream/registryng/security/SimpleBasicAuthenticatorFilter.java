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
package org.tomitribe.tribestream.registryng.security;

import org.tomitribe.tribestream.registryng.resources.LoginResource;

import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * we don't want WWW-Authenticate header for client app
 * + ensure we are stateless whatever the server config is (no session)
 */
@WebFilter(urlPatterns = "/api/*")
public class SimpleBasicAuthenticatorFilter implements Filter {

    private static final Logger LOGGER = Logger.getLogger(LoginResource.class.getName());

    private static final String BASIC = "basic ";

    @Inject
    private LoginContext loginContext;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) throws IOException, ServletException {
        if (HttpServletRequest.class.isInstance(request)) {
            final HttpServletRequest req = HttpServletRequest.class.cast(request);

            if (req.getRequestURL().toString().endsWith("/login")
                ||req.getRequestURL().toString().endsWith("/server/info")) {
                chain.doFilter(request, response);
                return;
            }

            final String auth = req.getHeader("Authorization");
            if (auth != null && auth.toLowerCase(Locale.ENGLISH).startsWith(BASIC)) {
                final String value = auth.substring(BASIC.length());
                final String decoded = new String(DatatypeConverter.parseBase64Binary(value));
                if (decoded.contains(":")) {
                    final String[] usernamePassword = decoded.split(":");
                    final String username = usernamePassword[0];
                    final String password = usernamePassword[1];
                    try {
                        req.login(username, password);

                        LOGGER.log(Level.FINE, () -> String.format("Successfully logged in '%s' for request '%s'", username, req.getRequestURI()));
                    } catch (final ServletException se) {
                        LOGGER.log(Level.WARNING, se, () -> String.format("Login failed for user '%s' for request '%s'", username, req.getRequestURI()));
                        // no-op, let it be a 401
                        final HttpServletResponse resp = HttpServletResponse.class.cast(response);
                        //resp.setHeader("WWW-Authenticate", value.toString()); // that's what we don't want
                        resp.sendError(HttpServletResponse.SC_UNAUTHORIZED);
                        return;
                    }

                    loginContext.setUsername(usernamePassword[0]);
                    try {
                        chain.doFilter(request, response);
                    } finally {
                        req.logout();
                    }
                    return;
                }
            }
            HttpServletResponse.class.cast(response).sendError(HttpServletResponse.SC_UNAUTHORIZED);

        }
    }

    @Override
    public void destroy() {
        // no-op
    }
}
