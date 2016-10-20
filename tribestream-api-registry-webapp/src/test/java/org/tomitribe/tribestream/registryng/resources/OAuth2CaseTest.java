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
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.IntrospectResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.OAuth2Tokens;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.inject.Inject;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED_TYPE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.rules.RuleChain.outerRule;

public class OAuth2CaseTest {
    @Application
    private Registry registry;

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @Tribe
    @Inject
    private CacheManager cacheManager;

    @Test
    public void authenticate() {
        // we get a valid token using oauth2
        final AccessTokenResponse token = registry.target(false)
                .path("api/security/oauth2")
                .request()
                .accept(APPLICATION_JSON_TYPE)
                .post(entity(new Form()
                                .param("username", "useroauth2")
                                .param("password", "passwordoauth2"),
                        APPLICATION_FORM_URLENCODED_TYPE), AccessTokenResponse.class);
        assertNotNull("we have an access token", token.getAccessToken());

        // then we do a secured request to ensure this token works
        assertEquals(Response.Status.OK.getStatusCode(), registry.target(false)
                .path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                .get().getStatus());

        // then we check it is in cache
        final Cache<OAuth2Tokens.TokenKey, IntrospectResponse> cache = cacheManager.getCache(
                "tribestream-api-registry.security.tokens", OAuth2Tokens.TokenKey.class, IntrospectResponse.class);
        final OAuth2Tokens.TokenKey key = new OAuth2Tokens.TokenKey(token.getAccessToken());
        final IntrospectResponse value = cache.get(key);
        assertNotNull(value);
        // evict it
        cache.remove(key);

        // redoing a request will retrigger introspect and therefore will still work
        assertEquals(Response.Status.OK.getStatusCode(), registry.target(false)
                .path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .header("Authorization", token.getTokenType() + " " + token.getAccessToken())
                .get().getStatus());

        cache.clear(); // avoid side effects for other tests
    }
}
