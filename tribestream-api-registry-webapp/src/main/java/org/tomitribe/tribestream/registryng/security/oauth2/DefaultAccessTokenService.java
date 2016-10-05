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
package org.tomitribe.tribestream.registryng.security.oauth2;

import javax.enterprise.context.ApplicationScoped;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@ApplicationScoped
public class DefaultAccessTokenService implements AccessTokenService {

    private static class TokenHolder {

        private final AccessTokenResponse accessTokenResponse;

        private final long creationDate;


        private TokenHolder(AccessTokenResponse accessTokenResponse) {
            this.accessTokenResponse = accessTokenResponse;
            this.creationDate = System.currentTimeMillis();
        }

        public boolean isExpired() {
            long now = System.currentTimeMillis();

            long expiryTs = creationDate + accessTokenResponse.getExpiresIn() * 1000;

            return expiryTs < now;
        }
    }

    private ConcurrentMap<String, TokenHolder> tokenCache = new ConcurrentHashMap<>();


    @Override
    public void addAccessToken(final AccessTokenResponse tokenResponse){
        System.out.println("Add accesstoken " + tokenResponse);
        tokenCache.put(tokenResponse.getAccessToken(), new TokenHolder(tokenResponse));

    }

    @Override
    public boolean hasToken(final String accessToken) {

        TokenHolder tokenHolder = tokenCache.get(accessToken);

        if (tokenHolder == null) {
            return false;
        } else if (tokenHolder.isExpired()) {
            tokenCache.remove(accessToken);
            return false;
        } else {
            return true;
        }

    }

}
