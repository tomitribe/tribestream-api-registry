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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.openejb.jee.JaxbJavaee;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.jee.jpa.unit.Persistence;
import org.apache.openejb.jee.jpa.unit.PersistenceUnit;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.EnableServices;
import org.apache.openejb.testing.JaxrsProviders;
import org.apache.openejb.testing.Module;
import org.apache.openejb.testing.RandomPort;
import org.hibernate.jpa.boot.scan.spi.ScanOptions;
import org.hibernate.jpa.boot.scan.spi.ScanResult;
import org.hibernate.jpa.boot.scan.spi.Scanner;
import org.hibernate.jpa.boot.spi.ClassDescriptor;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;
import org.hibernate.jpa.boot.spi.PackageDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;
import org.tomitribe.tribestream.registryng.bootstrap.Bootstrap;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.security.LoginContext;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import org.tomitribe.tribestream.registryng.webapp.RegistryNgApplication;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Collections;
import java.util.Set;

/**
 * Contains the configuration for the various tests in this package
 */
@EnableServices("jaxrs")
public class Application {

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper objectMapper;

    @Module
    @JaxrsProviders(CustomJacksonJaxbJsonProvider.class)
    @Classes(cdi = true, value = {
            Repository.class, SwaggerJsonMapperProducer.class,
            Bootstrap.class, SearchEngine.class,
            SearchResource.class, RegistryResource.class,
            ApplicationResource.class,
            LoginContext.class,
            RegistryNgApplication.class
    })
    public WebApp war() throws Exception {
        return new WebApp();
    }

    @Module
    public PersistenceUnit jpa() throws Exception {
        try (final InputStream fis = new FileInputStream("src/main/resources/META-INF/persistence.xml")) {
            final PersistenceUnit unit = Persistence.class.cast(JaxbJavaee.unmarshal(Persistence.class, fis, false)).getPersistenceUnit().iterator().next();
            unit.getProperties().clear();
            unit.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            unit.setProperty("hibernate.ejb.resource_scanner", NoScanning.class.getName()); // we run in parallel so folders can be visited and deleted in parallel
            return unit;
        }
    }

    public static class NoScanning implements Scanner {
        @Override
        public ScanResult scan(PersistenceUnitDescriptor persistenceUnitDescriptor, ScanOptions scanOptions) {
            return new ScanResult() {
                @Override
                public Set<PackageDescriptor> getLocatedPackages() {
                    return Collections.emptySet();
                }

                @Override
                public Set<ClassDescriptor> getLocatedClasses() {
                    return Collections.emptySet();
                }

                @Override
                public Set<MappingFileDescriptor> getLocatedMappingFiles() {
                    return Collections.emptySet();
                }
            };
        }
    }

    @RandomPort("http")
    private int port;

    public int getPort() {
        return port;
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }

    protected Client getClient() {
        return ClientBuilder.newBuilder()
                .property("skip.default.json.provider.registration", true)
                .register(new CustomJacksonJaxbJsonProvider())
                .build();
    }

}
