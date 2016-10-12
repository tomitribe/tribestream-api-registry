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

import org.tomitribe.tribestream.registryng.entities.AccessToken;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class DefaultAccessTokenService implements AccessTokenService {

    public static final Logger LOG = Logger.getLogger(DefaultAccessTokenService.class.getName());

    @PersistenceContext
    private EntityManager em;

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
        LOG.fine("Add new access token " + tokenResponse);
        AccessToken accessTokenEntity = new AccessToken();
        accessTokenEntity.setAccessToken(tokenResponse.getAccessToken());
        accessTokenEntity.setExpiryTimestamp(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
        accessTokenEntity.setScope(tokenResponse.getScope());
        em.persist(accessTokenEntity);
    }

    @Override
    public List<String> getScopes(final String accessToken) throws InvalidTokenException {

        AccessToken accessTokenEntity = em.find(AccessToken.class, accessToken);
        if (accessTokenEntity == null) {
            throw new InvalidTokenException("Could not find access token: " + accessToken);
        }
        if (accessTokenEntity.getExpiryTimestamp() < System.currentTimeMillis()) {
            throw new InvalidTokenException("Found expired access token! " + accessTokenEntity.getAccessToken());
        } else {
            return Stream.of(accessTokenEntity.getScope().split("\\s+")).collect(toList());
        }
    }

    @Override
    public void deleteToken(final String accessToken) {
        AccessToken accessTokenEntity = em.getReference(AccessToken.class, accessToken);
        em.remove(accessTokenEntity);
    }

    @Schedule(minute="*", hour="*")
    public void timedDeleteExpiredTokens() {
        LOG.fine("Delete expired tokens");
        int deletedTokens = deleteExpiredTokens();
        if (deletedTokens > 0) {
            LOG.info("Purged " + deletedTokens + " expired tokens");
        }
    }

    public int deleteExpiredTokens() {
        return em.createNamedQuery(AccessToken.Queries.DELETE_EXPIRED_TOKENS)
                .setParameter("now", System.currentTimeMillis())
                .executeUpdate();
    }

}
