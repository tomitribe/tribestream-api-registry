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
package com.tomitribe.tribestream.registryng.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomitribe.tribestream.registryng.bootstrap.Bootstrap;
import com.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import com.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import com.tomitribe.tribestream.registryng.repository.Repository;
import com.tomitribe.tribestream.registryng.service.search.SearchEngine;
import com.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import com.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import com.tomitribe.tribestream.registryng.webapp.RegistryNgApplication;
import com.tomitribe.tribestream.test.registryng.category.Embedded;
import com.tomitribe.tribestream.test.registryng.util.DefaultContainer;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposerRule;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.EnableServices;
import org.apache.openejb.testing.JaxrsProviders;
import org.apache.openejb.testing.Module;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@Category(Embedded.class)
@EnableServices("jaxrs")
public class ApplicationResourceTest extends AbstractResourceTest {

    private final DefaultContainer container = new DefaultContainer(true);

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper objectMapper;

    @Rule
    public final ApplicationComposerRule app = new ApplicationComposerRule(this, container);

    @Module
    @JaxrsProviders(CustomJacksonJaxbJsonProvider.class)
    @Classes(cdi = true, value = {
        Repository.class, SwaggerJsonMapperProducer.class,
        Bootstrap.class, SearchEngine.class,
        ApplicationResource.class, RegistryNgApplication.class
    })
    public WebApp war() throws Exception {
        return new WebApp();
    }

    @Test
    public void shouldLoadAllApplications() throws Exception {
        List<ApplicationWrapper> apps = loadAllApplications();

        assertEquals(2, apps.size());
        assertEquals("Swagger Petstore", apps.get(0).getSwagger().getInfo().getTitle());
        assertEquals("Uber API", apps.get(1).getSwagger().getInfo().getTitle());
    }

    @Test
    public void shouldLoadApplicationFromLink() throws Exception {

        final List<ApplicationWrapper> apps = loadAllApplications();

        final WebClient directAppClient = WebClient.create(apps.get(1).getLinks().get("self"), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE);
        final ApplicationWrapper directApplicationResponse = directAppClient.get(ApplicationWrapper.class);

        assertEquals(apps.get(1).getSwagger().getInfo().getTitle(), directApplicationResponse.getSwagger().getInfo().getTitle());
        assertEquals(apps.get(1).getSwagger().getInfo().getVersion(), directApplicationResponse.getSwagger().getInfo().getVersion());
    }

    @Test
    public void shouldLoadEndpointAsSubresourceFromApplication() throws Exception {

        final List<ApplicationWrapper> apps = loadAllApplications();

        final Map.Entry<String, Path> pathEntry = apps.get(0).getSwagger().getPaths().entrySet().iterator().next();

        final String path = pathEntry.getKey();

        final Map.Entry<HttpMethod, Operation> operationEntry = pathEntry.getValue().getOperationMap().entrySet().iterator().next();

        final WebClient endpointClient = WebClient.create(apps.get(0).getLinks().get("self"), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .path(operationEntry.getKey().name().toLowerCase())
            .path(path.substring(1))
            .accept(MediaType.APPLICATION_JSON_TYPE);

        EndpointWrapper ep = endpointClient.get(EndpointWrapper.class);
        assertEquals(200, endpointClient.getResponse().getStatus());
        assertNotNull(ep);
    }

    @Test
    public void shouldImportOpenAPIDocument() throws Exception {

        final Swagger swagger = objectMapper.readValue(getClass().getResourceAsStream("/api-with-examples.json"), Swagger.class);
        final ApplicationWrapper request = new ApplicationWrapper(swagger);

        WebClient webClient = WebClient.create("http://localhost:" + container.getPort() + "/openejb/api/application", Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .type(MediaType.APPLICATION_JSON_TYPE);

        ApplicationWrapper applicationWrapper = webClient.post(request, ApplicationWrapper.class);
        assertEquals(201, webClient.getResponse().getStatus());

        assertEquals("List API versions", applicationWrapper.getSwagger().getPaths().get("/").getGet().getSummary());
        assertEquals("Show API version details", applicationWrapper.getSwagger().getPaths().get("/v2").getGet().getSummary());

        WebClient endpointWebclient = WebClient.create(applicationWrapper.getLinks().get("self"), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE)
            .path("get");

        EndpointWrapper endpoint = endpointWebclient.get(EndpointWrapper.class);
        assertEquals(Arrays.asList("application/json"), endpoint.getOperation().getProduces());

    }

    private List<ApplicationWrapper> loadAllApplications() {
        WebClient webClient = WebClient.create("http://localhost:" + container.getPort(), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE);

        return new ArrayList<>(webClient.path("/openejb/api/application").getCollection(ApplicationWrapper.class));
    }

}
