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
package com.tomitribe.tribestream.registryng.bootstrap;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import com.tomitribe.tribestream.registryng.repository.Repository;
import com.tomitribe.tribestream.registryng.service.search.SearchEngine;
import com.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import io.swagger.models.Swagger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.ejb.DependsOn;
import javax.ejb.EJB;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.FilenameFilter;
import java.net.URL;

/**
 * Seeds the database at startup with Swagger documents stored in META-INF/classes/seed-db.
 */
@Singleton
@Startup
@DependsOn("SearchEngine")
public class Bootstrap {

    private static final Logger LOGGER = LoggerFactory.getLogger(Bootstrap.class);

    @EJB
    private Repository repository;

    @EJB
    private SearchEngine searchEngine;

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper mapper;

    @PostConstruct
    public void init() {
        seedDatabase();

        searchEngine.resetIndex();
    }

    public void seedDatabase() {

        URL res = Thread.currentThread().getContextClassLoader().getResource("seed-db");
        if (!"file".equals(res.getProtocol())) {
            LOGGER.error("Cannot load initial OpenAPI documents!");
            return;
        }

        final File f = new File(res.getFile());
        final File[] swaggerFiles = f.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(final File dir, final String name) {
                return name.endsWith(".json");
            }
        });

        for (File swaggerFile: swaggerFiles) {
            seedFile(swaggerFile);
        }

    }

    private void seedFile(File swaggerFile) {
        LOGGER.info("Seeding " + swaggerFile.getName());

        try {
            final Swagger swagger = mapper.readValue(
                swaggerFile,
                Swagger.class
            );

            OpenApiDocument openApiDocument = repository.insert(swagger);
            LOGGER.info("Persisted application {}-{}", openApiDocument.getName(), openApiDocument.getVersion());

        } catch (Exception e) {
            LOGGER.warn("Seeding {} failed!", e, swaggerFile.getName());
        }
    }

}
