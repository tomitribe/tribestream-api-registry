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
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Seeds the database at startup with Swagger documents stored in META-INF/classes/seed-db.
 */
@Singleton
@Startup
@DependsOn("SearchEngine")
public class Provisioning {

    private static final Logger LOGGER = Logger.getLogger(Provisioning.class.getName());

    @EJB
    private Repository repository;

    @EJB
    private SearchEngine searchEngine;

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper mapper;

    @PostConstruct
    private void init() {
        seedDatabase();
        searchEngine.resetIndex();
    }

    private void seedDatabase() {
        final URL res = Thread.currentThread().getContextClassLoader().getResource("seed-db");
        if (res == null) {
            LOGGER.log(Level.WARNING, "Cannot find seed-db resource in the classpath.");
            return;
        }
        if (!"file".equals(res.getProtocol())) {
            LOGGER.log(Level.WARNING, "Cannot load initial OpenAPI documents because seed-db is at {0}!", res);
            return;
        }

        final File f = new File(res.getFile());
        final File[] swaggerFiles = f.listFiles((dir, name) -> name.endsWith(".json"));

        for (File swaggerFile: swaggerFiles) {
            seedFile(swaggerFile);
        }

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
    }

}
