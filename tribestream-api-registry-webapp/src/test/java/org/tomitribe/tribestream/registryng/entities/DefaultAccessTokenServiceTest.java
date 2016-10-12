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
package org.tomitribe.tribestream.registryng.entities;


import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenResponse;
import org.tomitribe.tribestream.registryng.security.oauth2.AccessTokenService;
import org.tomitribe.tribestream.registryng.test.Registry;

import javax.inject.Inject;
import java.util.UUID;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class DefaultAccessTokenServiceTest {

    @Application
    private Registry registry;

    @Inject
    private AccessTokenService accessTokenService;

    @Test
    public void shouldCreateAndFindAccessToken() {

        final String accessToken = UUID.randomUUID().toString();

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setExpiresIn(1000);

        accessTokenService.addAccessToken(tokenResponse);

        assertNotNull(accessTokenService.getToken(accessToken));
    }

    @Test
    public void shouldDeleteAccessToken() {

        // Given: a valid access token
        final String accessToken = UUID.randomUUID().toString();

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setExpiresIn(1000);

        accessTokenService.addAccessToken(tokenResponse);
        assertNotNull(accessTokenService.getToken(accessToken));

        // When: I delete it
        accessTokenService.deleteToken(accessToken);

        // Then: I also don't find it anymore
        assertNull(accessTokenService.getToken(accessToken));
    }

    @Test
    public void shouldNotFindInvalidAccessToken() {

        final String accessToken = UUID.randomUUID().toString();

        assertNull(accessTokenService.getToken(accessToken));
    }

    @Test
    public void shouldExpireTokens() throws Exception {

        // Given: A valid token
        final String accessToken = UUID.randomUUID().toString();

        AccessTokenResponse tokenResponse = new AccessTokenResponse();
        tokenResponse.setAccessToken(accessToken);
        tokenResponse.setExpiresIn(2);

        accessTokenService.addAccessToken(tokenResponse);
        assertNotNull(accessTokenService.getToken(accessToken));

        // When: the token expires
        Thread.sleep(3000);

        // Then: the Token is no longer found
        assertNull(accessTokenService.getToken(accessToken));

        // And: the Token will be deleted
        assertTrue(accessTokenService.deleteExpiredTokens() > 0);
    }

}
