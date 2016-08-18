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
package com.tomitribe.tribestream.test.registryng.util;

import org.apache.openejb.testing.Configuration;
import org.apache.openejb.testng.PropertiesBuilder;
import org.apache.openejb.util.Join;
import org.apache.openejb.util.NetworkUtil;

import java.util.Properties;

public class DefaultContainer {
    private final PropertiesBuilder builder;
    private final int port;

    public DefaultContainer(final String... exclusions) {
        this(false, exclusions);
    }

    public DefaultContainer(final boolean randomHttpEjbdPort, final String... exclusions) {
        builder = new PropertiesBuilder();

        if (randomHttpEjbdPort) {
            port = NetworkUtil.getNextAvailablePort();
            builder.p("httpejbd.port", Integer.toString(port));
        } else {
            port = -1;
        }

        builder.p("openejb.jul.forceReload", "true");
        if (exclusions != null && exclusions.length > 0) {
            builder.p("openejb.additional.exclude", Join.join(",", (Object[]) exclusions).replace("*", "tribestream-governance,tribestream-wadlx,tribestream-security"));
        }
    }

    @Configuration
    public Properties configuration() {
        return builder.build();
    }

    public DefaultContainer p(final String key, final String value) {
        builder.p(key, value);
        return this;
    }

    public int getPort() {
        return port;
    }
}
