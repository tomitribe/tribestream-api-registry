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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import org.tomitribe.tribestream.registryng.entities.AccessToken;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;

@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class AccessTokenService {
    private static final Logger LOG = Logger.getLogger(AccessTokenService.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Inject
    private Oauth2Configuration configuration;

    private final ObjectMapper mapper = new ObjectMapper();

    public void addAccessToken(final AccessTokenResponse tokenResponse) {
        LOG.fine("Add new access token " + tokenResponse);
        AccessToken accessTokenEntity = new AccessToken();
        accessTokenEntity.setAccessToken(tokenResponse.getAccessToken());
        accessTokenEntity.setExpiryTimestamp(System.currentTimeMillis() + tokenResponse.getExpiresIn() * 1000);
        accessTokenEntity.setScope(tokenResponse.getScope());
        accessTokenEntity.setUsername(tryExtractingTheUser(tokenResponse.getAccessToken()));
        em.persist(accessTokenEntity);
    }

    public AccessToken findToken(final String accessToken) throws InvalidTokenException {
        final List<AccessToken> tokens = em.createNamedQuery(AccessToken.Queries.FIND_BY_TOKEN, AccessToken.class).setParameter("token", accessToken).getResultList();
        AccessToken accessTokenEntity = tokens.isEmpty() ? null : tokens.iterator().next();
        if (accessTokenEntity == null) {
            throw new InvalidTokenException("Could not find access token: " + accessToken);
        }
        if (accessTokenEntity.getExpiryTimestamp() < System.currentTimeMillis()) {
            throw new InvalidTokenException("Found expired access token! " + accessTokenEntity.getAccessToken());
        }
        return accessTokenEntity;
    }

    public void deleteToken(final String accessToken) {
        em.remove(em.createNamedQuery(AccessToken.Queries.FIND_BY_TOKEN, AccessToken.class).setParameter("token", accessToken).getSingleResult());
    }

    @Schedule(minute = "*", hour = "*")
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

    private String tryExtractingTheUser(final String accessToken) {
        try {
            final String[] segments = accessToken.split("\\.");
            if (segments.length != 3) {
                return null;
            }
            JsonNode node = mapper.readTree(Base64.getUrlDecoder().decode(segments[1]));
            for (final String part : configuration.getJwtUsernameAttribute().split("\\/")) {
                node = node.get(part);
                if (node == null) {
                    return null;
                }
            }
            return ofNullable(node)
                    .filter(n -> n.getNodeType() == JsonNodeType.STRING)
                    .map(JsonNode::textValue)
                    .orElse(null);
        } catch (final RuntimeException | IOException re) {
            return null;
        }
    }
}
