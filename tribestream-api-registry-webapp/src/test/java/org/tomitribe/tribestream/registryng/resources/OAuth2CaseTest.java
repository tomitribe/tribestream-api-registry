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

import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.ws.rs.core.Form;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.rules.RuleChain.outerRule;

public class OAuth2CaseTest {
    @Application
    private Registry registry;

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @Test
    public void authenticate() {// TODO
        // we get a valid token using oauth2
        // then we
        final AccessTokenResponse token = registry.target(false)
                .path("api/security/oauth2")
                .request()
                .accept(APPLICATION_JSON_TYPE)
                .post(entity(new Form()
                                .param("username", "useroauth2")
                                .param("password", "passwordoauth2"),
                        APPLICATION_FORM_URLENCODED_TYPE), AccessTokenResponse.class);
    }
}
