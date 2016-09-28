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

import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.RandomPort;
import org.apache.openejb.testing.WebResource;
import org.apache.tomee.loader.TomcatHelper;
import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import org.tomitribe.util.Join;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import java.io.IOException;
import java.security.Principal;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.tomitribe.util.Join.join;

/**
 * Contains the configuration for the tests
 */
@ContainerProperties({
        @ContainerProperties.Property(name = "hibernate.hbm2ddl.auto", value = "create-drop"),
        @ContainerProperties.Property(name = "registryDatasource", value = "new://Resource?type=DataSource"),
        @ContainerProperties.Property(name = "registryDatasource.LogSql", value = "true")
})
@WebResource("src/main/webapp") // should work by default but bug in tomee 7.0.1, fixed in 7.0.2
@org.apache.openejb.testing.Application
public class Registry {
    public static final String TESTUSER = "utest";
    public static final String TESTPASSWORD = "ptest";
    @RandomPort("http")
    private int port;

    public WebTarget target() {
        return target(true);
    }

    public Client client() {
        return client(true);
    }

    public WebTarget target(final boolean secured) {
        return client(secured).target("http://localhost:" + port);
    }

    public Client client(final boolean secured) { // TODO: close them somehow, not a big deal for tests
        final Client client = ClientBuilder.newBuilder()
                .property("skip.default.json.provider.registration", true)
                .register(new CustomJacksonJaxbJsonProvider())
                .build();
        if (!secured) {
            return client;
        }
        return client.register(new ClientRequestFilter() {
            @Override
            public void filter(final ClientRequestContext requestContext) throws IOException {
                requestContext.getHeaders().put("Authorization", singletonList("Basic " + printBase64Binary(join(":", TESTUSER, TESTPASSWORD).getBytes("UTF-8"))));
            }
        });
    }

    @Dependent
    public static class SetRealm { // security mock for tests (depends the deployment)
        public void init(@Observes @Initialized(ApplicationScoped.class) final Object init) {
            TomcatHelper.getServer().findServices()[0].getContainer() // find the engine and set the realm
                    .setRealm(new RealmBase() {
                        @Override
                        protected String getName() {
                            return "registry-test";
                        }

                        @Override
                        protected String getPassword(final String username) {
                            return TESTUSER.equals(username) ? TESTPASSWORD : null;
                        }

                        @Override
                        protected Principal getPrincipal(final String username) {
                            return TESTUSER.equals(username) ? new GenericPrincipal(TESTUSER, TESTPASSWORD, emptyList()) : null;
                        }
                    });
        }
    }
}
