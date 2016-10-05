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
package org.tomitribe.tribestream.registryng.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Swagger;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;

/**
 * Seeds the database at startup with Swagger documents stored in META-INF/classes/seed-db.
 */
@Singleton
@Startup
@DependsOn("SearchEngine")
public class Provisioning {

    private static final Logger LOGGER = Logger.getLogger(Provisioning.class.getName());

    @Inject
    private Repository repository;

    @Inject
    private SearchEngine searchEngine;

    @Inject
    @Tribe
    private ObjectMapper mapper;

    @Inject // allow to switch it off or to use an external source for testing
    @ConfigProperty(name = "tribe.registry.seeding.location", defaultValue = "seed-db")
    private String location;

    @PostConstruct
    public void init() {
        if (location == null) {
            return;
        }
        seedDatabase();
        searchEngine.waitForWrites();
    }

    private void seedDatabase() {
        final File dir = new File(location);
        if (dir.isDirectory()) {
            doSeeding(dir);
            return;
        }

        // else try classpath

        final URL res = Thread.currentThread().getContextClassLoader().getResource(location);
        if (res == null) {
            LOGGER.log(Level.WARNING, "Cannot find seed-db resource in the classpath.");
            return;
        }
        if (!"file".equals(res.getProtocol())) {
            LOGGER.log(Level.WARNING, "Cannot load initial OpenAPI documents because seed-db is at {0}!", res);
            return;
        }
        doSeeding(new File(res.getFile()));

    }

    private void doSeeding(final File f) {
        Stream.of(ofNullable(f.listFiles((dir, name) -> name.endsWith(".json"))).orElseGet(() -> new File[0])).forEach(this::seedFile);
    }

    private void seedFile(final File swaggerFile) {
        LOGGER.info("Seeding " + swaggerFile.getName());

        try {
            final Swagger swagger = mapper.readValue(
                    swaggerFile,
                    Swagger.class
            );

            if (repository.findApplicationByNameAndVersion(swagger.getInfo().getTitle(), swagger.getInfo().getVersion()) == null) {
                OpenApiDocument openApiDocument = repository.insert(swagger);
                LOGGER.log(Level.INFO, "Persisted application {0}-{1}", new Object[]{openApiDocument.getName(), openApiDocument.getVersion()});

            } else {
                LOGGER.log(Level.INFO, "Application {0}-{1} already available in DB ", new Object[]{swagger.getInfo().getTitle(), swagger.getInfo().getVersion()});
            }

        } catch (final Exception e) {
            LOGGER.log(Level.WARNING, e, () -> String.format("Seeding %s failed!", swaggerFile.getName()));
        }
        LOGGER.info("Memory = " + ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
    }

}
