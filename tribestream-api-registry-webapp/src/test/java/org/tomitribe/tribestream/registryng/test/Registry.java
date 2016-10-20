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
package org.tomitribe.tribestream.registryng.test;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.util.DeserializationModule;
import org.apache.catalina.realm.GenericPrincipal;
import org.apache.catalina.realm.RealmBase;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.RandomPort;
import org.apache.openejb.testing.WebResource;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.apache.tomee.loader.TomcatHelper;
import org.openqa.selenium.WebDriver;
import org.tomitribe.tribestream.registryng.bootstrap.Provisioning;
import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import org.tomitribe.tribestream.registryng.test.elasticsearch.Elasticsearch;
import org.tomitribe.tribestream.registryng.test.logging.LoggingSetup;
import org.tomitribe.tribestream.registryng.test.selenium.PhantomJsLifecycle;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.Initialized;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.security.Principal;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static javax.xml.bind.DatatypeConverter.printBase64Binary;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.tomitribe.util.Join.join;

/**
 * Contains the configuration for the tests
 */
@ContainerProperties({
        @ContainerProperties.Property(name = "openejb.datasource.plugin.activated", value = "false"),
        @ContainerProperties.Property(name = "hibernate.hbm2ddl.auto", value = "create-drop"),
        @ContainerProperties.Property(name = "registryDatasource", value = "new://Resource?type=DataSource"),
        @ContainerProperties.Property(name = "registryDatasource.JdbcDriver", value = "org.h2.Driver"),
        @ContainerProperties.Property(name = "registryDatasource.JdbcUrl", value = "jdbc:h2:mem:registry;DB_CLOSE_ON_EXIT=FALSE"),
        @ContainerProperties.Property(name = "tribe.registry.elasticsearch.base", value = "http://localhost:${test.elasticsearch.port}"),
        @ContainerProperties.Property(name = "tribe.registry.monitoring.http.urls", value = "http://localhost:${test.elasticsearch.port}"),
        @ContainerProperties.Property(name = "tribe.registry.oauth2.authorizationServerUrl", value = "http://localhost:${tomee.embedded.http}/api/oauth2/token"),
        @ContainerProperties.Property(name = "tribe.registry.oauth2.introspectServerUrl", value = "http://localhost:${tomee.embedded.http}/api/oauth2/introspect"),
        @ContainerProperties.Property(
                name = "tribe.registry.security.filter.whitelist",
                value = "/api/server/info,/api/login,/api/security/oauth2,/api/security/oauth2/status,/api/oauth2/token,/api/oauth2/introspect")
        /* can help for debugging (dumps sql queries and ES client HTTP requests
        ,@ContainerProperties.Property(name = "registryDatasource.LogSql", value = "true"),
        @ContainerProperties.Property(name = "tribe.registry.elasticsearch.features", value = "org.apache.cxf.feature.LoggingFeature")
        */
})
@WebResource("target/tests-webapp")
@org.apache.openejb.testing.Application
@TomEEEmbeddedSingleRunner.LifecycleTasks({LoggingSetup.class, PrepareResources.class, Elasticsearch.class, PhantomJsLifecycle.Task.class})
public class Registry {
    public static final String TESTUSER = "utest";
    public static final String TESTPASSWORD = "ptest";

    @RandomPort("http")
    private int port;

    @Inject
    private Provisioning provisioning;

    private PhantomJsLifecycle phantomJs;

    public void withRetries(final Runnable task, final String... description) {
        withRetries(() -> {
            task.run();
            return null;
        });
    }

    public <T> T withRetries(final Supplier<T> task, final String... description) {
        Throwable lastErr = null;
        final int max = Integer.getInteger("test.registry.retries", 60);
        final Client client = ClientBuilder.newClient();
        try {
            for (int i = 0; i < max; i++) {
                assertEquals(
                        Response.Status.OK.getStatusCode(),
                        client.target("http://localhost:" + System.getProperty("test.elasticsearch.port"))
                                .path("_refresh")
                                .request(MediaType.APPLICATION_JSON_TYPE)
                                .get()
                                .getStatus());
                try {
                    return task.get();
                } catch (final Throwable error) {
                    lastErr = error;
                    if (i % 3 == 0) {
                        Logger.getLogger(Registry.class.getName()).info("Retry cause (" + (i + 1) + "/" + max + ")"
                                + ofNullable(description).filter(d -> d.length >= 1).map(d -> Stream.of(d).collect(joining(" "))).orElse("")
                                + ": " + error.getMessage());
                    }
                    try {
                        sleep(1000);
                    } catch (final InterruptedException e) {
                        Thread.interrupted();
                        fail("quitting");
                    }
                }
            }
        } finally {
            client.close();
        }
        if (RuntimeException.class.isInstance(lastErr)) {
            throw RuntimeException.class.cast(lastErr);
        }
        throw new IllegalStateException(lastErr);
    }

    public String root() {
        return "http://localhost:" + port;
    }

    public WebDriver getWebDriver() { // lazy for tests not needing it
        return PhantomJsLifecycle.SINGLETON.getDriver();
    }

    public WebTarget target() {
        return target(true);
    }

    public Client client() {
        return client(true);
    }

    public WebTarget target(final boolean secured) {
        return client(secured).target(root());
    }

    public Client client(final boolean secured) { // TODO: close them somehow, not a big deal for tests
        final Client client = ClientBuilder.newBuilder()
                .property("skip.default.json.provider.registration", true)
                .register(new CustomJacksonJaxbJsonProvider(new ObjectMapper() {{
                    registerModule(new DeserializationModule(true, true));
                    setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
                }}) {
                })
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

    public void restoreData() {
        provisioning.restore();
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
