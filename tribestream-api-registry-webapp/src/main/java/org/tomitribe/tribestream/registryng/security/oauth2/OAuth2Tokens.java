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

import lombok.extern.java.Log;
import org.tomitribe.tribestream.registryng.jcache.ConfigurableCacheFactory;

import javax.cache.annotation.CacheDefaults;
import javax.cache.annotation.CacheInvocationParameter;
import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheKeyGenerator;
import javax.cache.annotation.CacheKeyInvocationContext;
import javax.cache.annotation.CacheResult;
import javax.cache.annotation.GeneratedCacheKey;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.lang.annotation.Annotation;

@Log
@ApplicationScoped
@CacheDefaults(
        cacheName = "tribestream-api-registry.security.tokens",
        cacheResolverFactory = ConfigurableCacheFactory.class,
        cacheKeyGenerator = OAuth2Tokens.TokenKeyGenerator.class)
public class OAuth2Tokens {
    @Inject
    private OAuth2Client client;

    @CacheResult(skipGet = true)
    public IntrospectResponse save(@CacheKey final AccessTokenResponse responsePayload) {
        final Response response = client.introspect(responsePayload.getAccessToken());
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.warning("Invalid token: '" + responsePayload.getAccessToken() + "'");
            return null;
        }

        final IntrospectResponse introspect = response.readEntity(IntrospectResponse.class);
        if (introspect.getActive() != null && !introspect.getActive()) {
            log.info("Inactive token: '" + responsePayload.getAccessToken() + "'");
            return null;
        }
// note that this is BROKEN cause username is NOT there (see security web filter)
        return introspect;
    }

    @CacheResult
    public IntrospectResponse find(@CacheKey final String accessToken) {
        final Response response = client.introspect(accessToken);
        if (response.getStatus() != Response.Status.OK.getStatusCode()) {
            log.warning("Invalid token: '" + accessToken + "'");
            return null;
        }

        final IntrospectResponse introspect = response.readEntity(IntrospectResponse.class);
        if (introspect.getActive() != null && !introspect.getActive()) {
            log.info("Inactive token: '" + accessToken + "'");
            return null;
        }
        return introspect;
    }

    // key can be a token response or string to keep java methods more friendly
    @ApplicationScoped
    public static class TokenKeyGenerator implements CacheKeyGenerator {
        @Override
        public GeneratedCacheKey generateCacheKey(final CacheKeyInvocationContext<? extends Annotation> cacheKeyInvocationContext) {
            final CacheInvocationParameter param = cacheKeyInvocationContext.getKeyParameters()[0];
            return new TokenKey(param.getRawType() == String.class ? String.class.cast(param.getValue()) : AccessTokenResponse.class.cast(param.getValue()).getAccessToken());
        }
    }

    public static class TokenKey implements GeneratedCacheKey, Serializable {
        private String value;

        public TokenKey(final String accessToken) {
            this.value = accessToken;
        }

        @Override
        public boolean equals(final Object o) {
            return this == o || !(o == null || getClass() != o.getClass()) && value.equals(TokenKey.class.cast(o).value);

        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }
    }
}
