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
package org.tomitribe.tribestream.registryng.jcache;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.documentation.Description;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.cache.Cache;
import javax.cache.CacheException;
import javax.cache.CacheManager;
import javax.cache.Caching;
import javax.cache.annotation.CacheInvocationContext;
import javax.cache.annotation.CacheMethodDetails;
import javax.cache.annotation.CacheResolver;
import javax.cache.annotation.CacheResolverFactory;
import javax.cache.annotation.CacheResult;
import javax.cache.configuration.FactoryBuilder;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.spi.CachingProvider;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import javax.inject.Inject;
import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.concurrent.TimeUnit;

import static java.util.Optional.ofNullable;

@ApplicationScoped
public class ConfigurableCacheFactory implements CacheResolverFactory {
    @Tribe
    @Produces
    private CacheManager cacheManager;

    private CachingProvider provider;

    @Inject
    @Description("Should OAuth2 tokens be cached")
    @ConfigProperty(name = "tribe.registry.security.token.cache.skip", defaultValue = "false")
    private Boolean skip;

    @Inject
    @Description("How many time from the moment the token is received it is considered as valid")
    @ConfigProperty(name = "tribe.registry.security.token.cache.expiry", defaultValue = "10 minutes")
    private String duration;

    private final CacheResolver noCacheResolver = new CacheResolver() {
        private final Cache cache = Cache.class.cast(Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{Cache.class},
                (proxy, method, args) -> null));

        @Override
        public <K, V> Cache<K, V> resolveCache(final CacheInvocationContext<? extends Annotation> cacheInvocationContext) {
            return cache;
        }
    };

    @PostConstruct
    private void init() {
        provider = Caching.getCachingProvider();
        cacheManager = provider.getCacheManager(provider.getDefaultURI(), provider.getDefaultClassLoader());
    }

    @PreDestroy
    private void destroy() {
        ofNullable(cacheManager).ifPresent(CacheManager::close);
        ofNullable(provider).ifPresent(CachingProvider::close);
    }

    @Override
    public CacheResolver getCacheResolver(final CacheMethodDetails<? extends Annotation> cacheMethodDetails) {
        return skip ? noCacheResolver : findCacheResolver(cacheMethodDetails.getCacheName());
    }

    @Override
    public CacheResolver getExceptionCacheResolver(final CacheMethodDetails<CacheResult> cacheMethodDetails) {
        final String exceptionCacheName = cacheMethodDetails.getCacheAnnotation().exceptionCacheName();
        if (exceptionCacheName.isEmpty()) {
            throw new IllegalArgumentException("CacheResult.exceptionCacheName() not specified");
        }
        return skip ? noCacheResolver : findCacheResolver(exceptionCacheName);
    }

    private CacheResolver findCacheResolver(final String name) {
        Cache<?, ?> cache = cacheManager.getCache(name);
        if (cache == null) {
            try {
                cache = createCache(name);
            } catch (final CacheException ce) {
                cache = cacheManager.getCache(name);
            }
        }
        return new ConfiguredCacheResolver(cache);
    }

    private Cache<?, ?> createCache(final String exceptionCacheName) {
        final org.tomitribe.util.Duration d = new org.tomitribe.util.Duration(duration, TimeUnit.MILLISECONDS);
        cacheManager.createCache(exceptionCacheName, new MutableConfiguration<>()
                .setStoreByValue(false)
                .setExpiryPolicyFactory(new FactoryBuilder.SingletonFactory<>(new CreatedExpiryPolicy(new Duration(d.getUnit(), d.getTime())))));
        return cacheManager.getCache(exceptionCacheName);
    }

    private static class ConfiguredCacheResolver implements CacheResolver {
        private final Cache<?, ?> cache;

        private ConfiguredCacheResolver(final Cache<?, ?> cache) {
            this.cache = cache;
        }

        @Override
        public <K, V> Cache<K, V> resolveCache(final CacheInvocationContext<? extends Annotation> cacheInvocationContext) {
            return Cache.class.cast(cache);
        }
    }
}
