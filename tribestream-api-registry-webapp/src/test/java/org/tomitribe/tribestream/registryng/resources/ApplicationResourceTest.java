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


import io.swagger.models.HttpMethod;
import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import org.apache.commons.codec.EncoderException;
import org.apache.commons.codec.net.URLCodec;
import org.apache.openejb.junit.ApplicationComposerRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.test.category.Embedded;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@Category(Embedded.class)
public class ApplicationResourceTest {

    @ClassRule
    public final static ApplicationComposerRule app = new ApplicationComposerRule(new Application());

    @Test
    public void shouldLoadAllApplications() throws Exception {
        List<String> applicationNames =
                loadAllApplications().stream()
                .map(ApplicationWrapper::getSwagger)
                .map(Swagger::getInfo)
                .map(Info::getTitle)
                .collect(toList());

        assertThat(applicationNames, hasItems("Swagger Petstore", "Uber API"));
    }

    @Test
    public void shouldLoadApplicationFromLink() throws Exception {

        final List<ApplicationWrapper> apps = loadAllApplications();

        final ApplicationWrapper directApplicationResponse = getApp().getClient().target(apps.get(1).getLinks().get("self"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);

        assertEquals(apps.get(1).getSwagger().getInfo().getTitle(), directApplicationResponse.getSwagger().getInfo().getTitle());
        assertEquals(apps.get(1).getSwagger().getInfo().getVersion(), directApplicationResponse.getSwagger().getInfo().getVersion());
    }

    @Test
    public void shouldLoadEndpointAsSubresourceFromApplication() throws Exception {

        final List<ApplicationWrapper> apps = loadAllApplications();

        final Map.Entry<String, Path> pathEntry = apps.get(0).getSwagger().getPaths().entrySet().iterator().next();

        final String path = pathEntry.getKey();

        final Map.Entry<HttpMethod, Operation> operationEntry = pathEntry.getValue().getOperationMap().entrySet().iterator().next();

        EndpointWrapper ep = getClient().target(apps.get(0).getLinks().get("self"))
                .path(operationEntry.getKey().name().toLowerCase())
                .path(new URLCodec("utf-8").encode(path.substring(1)))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
        assertNotNull(ep);
    }

    @Test
    public void shouldImportOpenAPIDocument() throws Exception {

        try {
            // Given: n Applications are installed and a new Swagger document to import
            final int oldApplicationCount = loadAllApplications().size();

            final Swagger swagger = app.getInstance(Application.class).getObjectMapper().readValue(getClass().getResourceAsStream("/api-with-examples.json"), Swagger.class);
            final ApplicationWrapper request = new ApplicationWrapper(swagger);

            // When: The Swagger document is posted to the application resource

            final Response response = getClient().target("http://localhost:" + getPort() + "/openejb/api/application")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .buildPost(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE))
                    .invoke();

            // Then: The response status 201 and contains the imported document
            assertEquals(201, response.getStatus());

            final ApplicationWrapper applicationWrapper = response.readEntity(ApplicationWrapper.class);

            assertEquals("List API versions", applicationWrapper.getSwagger().getPaths().get("/").getGet().getSummary());
            assertEquals("Show API version details", applicationWrapper.getSwagger().getPaths().get("/v2").getGet().getSummary());

            // And: the response document contains the link to itself
            EndpointWrapper endpoint = getClient().target(applicationWrapper.getLinks().get("self")).path("get")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(EndpointWrapper.class);

            assertEquals(Arrays.asList("application/json"), endpoint.getOperation().getProduces());

            // And: When loading all applications the number of applications has increased by 1
            assertEquals(oldApplicationCount + 1, loadAllApplications().size());

            // And: The search also returns the two imported endpoints
            SearchPage searchPage = getClient().target("http://localhost:" + getPort() + "/openejb/api/search")
                    .queryParam("tag", "test")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(SearchPage.class);

            assertEquals(2, searchPage.getResults().size());
            final List<String> foundPaths = searchPage.getResults().stream()
                    .map(SearchResult::getPath)
                    .collect(toList());
            assertThat(foundPaths, both(hasItem("/")).and(hasItem("/v2")));
        } catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    @Test
    public void should_create_and_update_application() throws IOException {
        // Given: A new service is created
        final String initialDocument = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"Test API\",\n" +
                "    \"version\": \"v2\"\n" +
                "  }\n" +
                "}";
        final Swagger createSwagger = app.getInstance(Application.class).getObjectMapper().readValue(initialDocument, Swagger.class);
        final ApplicationWrapper createRequest = new ApplicationWrapper(createSwagger);
        final ApplicationWrapper newApplicationWrapper = getClient().target("http://localhost:" + getPort() + "/openejb/api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPost(Entity.entity(createRequest, MediaType.APPLICATION_JSON_TYPE))
                .invoke(ApplicationWrapper.class);
        assertNotNull(newApplicationWrapper);

        final String applicationURL = newApplicationWrapper.getLinks().get("self");
        // When: I add tags and a path to the application
        final String updateDocument = "{\n" +
                "  \"paths\": {\n" +
                "    \"pets\": {\n" +
                "      \"get\": {\n" +
                "        \"description\": \"Description for get pets\"\n" +
                "      }\n" +
                "    }\n" +
                "  },\n" +
                "  \"tags\": [ \n" +
                "    {\n" +
                "      \"name\": \"Tag1\",\n" +
                "      \"description\": \"Description for Tag1\"\n" +
                "    },\n" +
                "      {\n" +
                "      \"name\": \"Tag2\",\n" +
                "      \"description\": \"Description for Tag2\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        final Swagger updateSwagger = app.getInstance(Application.class).getObjectMapper().readValue(updateDocument, Swagger.class);
        final ApplicationWrapper updateRequest = new ApplicationWrapper(updateSwagger);

        ApplicationWrapper updatedApplicationWrapper = getClient().target(applicationURL)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPut(Entity.entity(updateRequest, MediaType.APPLICATION_JSON_TYPE))
                .invoke(ApplicationWrapper.class);

        // Then: The old information is still present
        assertNotNull(updatedApplicationWrapper);
        assertEquals(applicationURL, updatedApplicationWrapper.getLinks().get("self"));
        assertEquals("Test API", updatedApplicationWrapper.getSwagger().getInfo().getTitle());
        assertEquals("v2", updatedApplicationWrapper.getSwagger().getInfo().getVersion());

        // And: The new information is applied as well
        assertNotNull(updatedApplicationWrapper.getSwagger().getTags());
        assertEquals(2, updatedApplicationWrapper.getSwagger().getTags().size());
        assertThat(updatedApplicationWrapper.getSwagger().getTags().stream().map(Tag::getName).collect(toList()), hasItems("Tag1", "Tag2"));

        // TODO: Paths not handled yet!
    }

    @Test
    public void should_fail_with_bad_request_on_import_of_bad_document() throws IOException {
        // Given: A new service is created
        final String initialDocument = "{\n" +
                "  \"swagger\": {\n" +
                "    \"swagger\": \"2.0\"\n" +
                "  }\n" +
                "}";
        final Response response = getClient().target("http://localhost:" + getPort() + "/openejb/api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPost(Entity.entity(initialDocument, MediaType.APPLICATION_JSON_TYPE))
                .invoke();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }


    private List<ApplicationWrapper> loadAllApplications() {
        List<ApplicationWrapper> result = getClient().target("http://localhost:" + getPort() + "/openejb/api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<ApplicationWrapper>>() {
                });

        return result;
    }

    private Application getApp() {
        return app.getInstance(Application.class);
    }

    private int getPort() {
        return getApp().getPort();
    }

    public Client getClient() {
        return getApp().getClient();
    }
}
