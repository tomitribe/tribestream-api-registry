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
package org.tomitribe.tribestream.registryng.cdi;

import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.deltaspike.core.api.config.ConfigResolver;
import org.apache.deltaspike.core.spi.config.ConfigFilter;

import javax.enterprise.event.Observes;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ConfigSetupExtension implements Extension {
    void init(@Observes final BeforeBeanDiscovery bbd) {
        try {
            ConfigResolver.addConfigFilter(new TomEEFilter());
        } catch (final Throwable th) {
            // skip
        }
    }

    private static class TomEEFilter implements ConfigFilter {
        private static final Method DECIPHERING_METHOD;

        static {
            try {
                DECIPHERING_METHOD = TomEEFilter.class.getClassLoader().loadClass("org.apache.openejb.util.PropertyPlaceHolderHelper")
                        .getMethod("simpleValue", String.class);
            } catch (final NoSuchMethodException | ClassNotFoundException e) {
                throw new IllegalStateException("Not in TomEE so deciphering feature will not be available");
            }
        }

        @Override
        public String filterValue(final String key, final String value) {
            if (ConfigProperty.NULL.equals(value)) {
                return null;
            }

            try {
                return String.class.cast(DECIPHERING_METHOD.invoke(null, value));
            } catch (final IllegalAccessException e) {
                throw new IllegalStateException(e);
            } catch (final InvocationTargetException e) {
                throw new IllegalArgumentException(e.getCause());
            }
        }

        @Override
        public String filterValueForLog(final String key, final String value) {
            return key.endsWith("password") ? "xxxxxxxx" : value;
        }
    }
}
