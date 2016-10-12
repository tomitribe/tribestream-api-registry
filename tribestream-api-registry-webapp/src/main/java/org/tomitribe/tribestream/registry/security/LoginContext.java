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
package org.tomitribe.tribestream.registry.security;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import java.security.Principal;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class LoginContext {
    @Inject
    private HttpServletRequest request;

    public String getUsername() {
        Principal userPrincipal = null;
        try {
            // if no request injected, this is probably provisioning or some batch operations.
            // let's set the user to a technical user - we can't really test if request is null because it will never
            // really be - it's a proxy
            userPrincipal = request.getUserPrincipal();

        } catch (final NullPointerException npe) {
            return System.getProperty("tribe.revision.username", "tribe-provisioning");
        }

        return ofNullable(userPrincipal).map(Principal::getName)
                .orElseThrow(() -> new IllegalStateException("No user in current context"));
    }
}
