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
import io.swagger.models.Info;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.EntityLink;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.search.EndpointSearchResult;
import org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension;
import org.tomitribe.tribestream.registryng.domain.search.ApplicationSearchResult;
import org.tomitribe.tribestream.registryng.domain.search.SearchResult;
import org.tomitribe.tribestream.registryng.entities.Normalizer;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.test.Registry;

import javax.inject.Inject;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class ApplicationResourceTest {
    @Inject
    @Tribe
    private ObjectMapper objectMapper;

    @Application
    private Registry registry;

    @Inject
    private SearchEngine engine;

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test(expected = NotAuthorizedException.class)
    public void applicationEndpointsAreSecured() throws Exception {
        registry.target(false).path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<ApplicationWrapper>>() {
                });
    }

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
    public void shouldImportOpenAPIDocument() throws Exception {

        try {
            // Given: n Applications are installed and a new Swagger document to import
            final int oldApplicationCount = loadAllApplications().size();

            final Swagger swagger = objectMapper.readValue(getClass().getResourceAsStream("/api-with-examples.json"), Swagger.class);
            final ApplicationWrapper request = new ApplicationWrapper(swagger, Normalizer.normalize(swagger.getInfo().getTitle()));

            // When: The Swagger document is posted to the application resource
            final Response response = registry.target().path("api/application")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .post(Entity.entity(request, MediaType.APPLICATION_JSON_TYPE));

            // Then: The response status 201 and contains the imported document
            assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

            final ApplicationWrapper applicationWrapper = response.readEntity(ApplicationWrapper.class);

            assertEquals("List API versions", applicationWrapper.getSwagger().getPaths().get("/").getGet().getSummary());
            assertEquals("Show API version details", applicationWrapper.getSwagger().getPaths().get("/v2").getGet().getSummary());

            // And: The response contains links for self, history and the two endpoints
            assertEquals(applicationWrapper.toString(), 5, getLinks(applicationWrapper).size());
            assertNotNull(getLink(applicationWrapper, "self"));
            assertNotNull(getLink(applicationWrapper, "history"));
            assertNotNull(getLink(applicationWrapper, "endpoints"));
            assertNotNull(getLink(applicationWrapper, "GET /"));
            assertNotNull(getLink(applicationWrapper, "GET /v2"));

            registry.withRetries(() -> {
                EndpointWrapper endpoint = getSearchPage().getResults().stream().map(SearchResult::getEndpoints).flatMap(List::stream)
                        .filter((EndpointSearchResult sr) -> "/v2".equals(sr.getPath()) && "GET".equals(sr.getHttpMethod()))
                        .findFirst()
                        .map((EndpointSearchResult sr) -> loadEndpoint(sr.getApplicationId(), sr.getEndpointId()))
                        .get();
                assertEquals(singletonList("application/json"), endpoint.getOperation().getProduces());

                // And: When loading all applications the number of applications has increased by 1
                assertEquals(oldApplicationCount + 1, loadAllApplications().size());

                // And: The search also returns the two imported endpoints
                SearchPage searchPage = registry.target().path("api/registry")
                        .queryParam("tag", "test")
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .get(SearchPage.class);

                assertEquals(2, searchPage.getResults().stream().map(SearchResult::getEndpoints).flatMap(List::stream).count());
                final List<String> foundPaths = searchPage.getResults().stream().map(SearchResult::getEndpoints).flatMap(List::stream)
                        .map(EndpointSearchResult::getPath)
                        .collect(toList());
                assertThat(foundPaths, both(hasItem("/")).and(hasItem("/v2")));
            }, "shouldImportOpenAPIDocument");
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
        final Swagger createSwagger = objectMapper.readValue(initialDocument, Swagger.class);
        final ApplicationWrapper createRequest = new ApplicationWrapper(createSwagger, null);
        final Response newApplicationWrapperResponse = registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createRequest, MediaType.APPLICATION_JSON_TYPE));

        ApplicationWrapper newApplicationWrapper = newApplicationWrapperResponse.readEntity(ApplicationWrapper.class);

        assertNotNull(getLink(newApplicationWrapper, "self"));

        assertNotNull(newApplicationWrapper);
        assertEquals("Test-API", newApplicationWrapper.getHumanReadableName());

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
        final Swagger updateSwagger = objectMapper.readValue(updateDocument, Swagger.class);
        final ApplicationWrapper updateRequest = new ApplicationWrapper(updateSwagger, null);

        final ApplicationWrapper updatedApplicationWrapper = registry.client().target(getLink(newApplicationWrapper, "self"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .put(Entity.entity(updateRequest, MediaType.APPLICATION_JSON_TYPE), ApplicationWrapper.class);

        // Then: The old information is still present
        assertNotNull(updatedApplicationWrapper);
        assertEquals("Test API", updatedApplicationWrapper.getSwagger().getInfo().getTitle());
        assertEquals("Test-API", updatedApplicationWrapper.getHumanReadableName());
        assertEquals("v2", updatedApplicationWrapper.getSwagger().getInfo().getVersion());

        // And: The new information is applied as well
        assertNotNull(updatedApplicationWrapper.getSwagger().getTags());
        assertEquals(2, updatedApplicationWrapper.getSwagger().getTags().size());
        assertThat(updatedApplicationWrapper.getSwagger().getTags().stream().map(Tag::getName).collect(toList()), hasItems("Tag1", "Tag2"));

        // TODO: Paths not handled yet!
    }

    @Test
    public void shouldDeleteApplication() throws Exception {
        final ApplicationSearchResult searchResult = registry.withRetries(() -> {
            final ApplicationSearchResult result = getSearchPage().getResults().get(0).getApplication();
            assertNotNull(loadApplication(result.getApplicationId()));
            return result;
        });
        final List<ApplicationWrapper> apps = loadAllApplications();
        final SearchPage sPageBefore = getSearchPage();
        final int endpointsCount = sPageBefore.getResults().get(0).getEndpoints().size();
        Response response = registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", searchResult.getApplicationId())
                .request()
                .delete();
        final SearchPage sPageAfter = getSearchPage();
        assertEquals("Wrong counter", sPageBefore.getTotal() - endpointsCount, sPageAfter.getTotal());
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        final List<ApplicationWrapper> newApps = loadAllApplications();
        assertEquals(apps.size() - 1, newApps.size());

        Response response2 = registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", searchResult.getApplicationId())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response2.getStatus());

        registry.withRetries(() -> assertFalse(registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class)
                .getResults().stream().map(SearchResult::getApplication)
                .filter(sr -> searchResult.getApplicationId().equals(sr.getApplicationId()))
                .findFirst()
                .isPresent()));
    }

    @Test
    public void shouldGet404WhenDeletingNotExistingApplication() throws Exception {
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", "whatever_doesNotExist")
                .request()
                .delete()
                .getStatus());
    }

    @Test
    public void should_fail_with_bad_request_on_import_of_bad_document() throws IOException {
        // Given: A new service is created
        final String initialDocument = "{\n" +
                "  \"swagger\": {\n" +
                "    \"swagger\": \"2.0\"\n" +
                "  }\n" +
                "}";
        final Response response = registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPost(Entity.entity(initialDocument, MediaType.APPLICATION_JSON_TYPE))
                .invoke();

        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
    }

    @Test
    public void should_not_create_duplicated_application() throws IOException {
        final String initialDocument = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"aaa\",\n" +
                "    \"version\": \"v2\"\n" +
                "  }\n" +
                "}";
        final Swagger createSwagger = objectMapper.readValue(initialDocument, Swagger.class);
        final ApplicationWrapper createRequest = new ApplicationWrapper(createSwagger, null);
        // create the app
        registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createRequest, MediaType.APPLICATION_JSON_TYPE));
        // create the same app again
        final Response newApplicationWrapperResponse = registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createRequest, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(400, newApplicationWrapperResponse.getStatus());
        assertEquals(
                "{\"key\": \"save.application.error\"}",
                new Scanner((InputStream) newApplicationWrapperResponse.getEntity()).useDelimiter("\\A").next()
        );
    }

    private List<ApplicationWrapper> loadAllApplications() {
        return registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<ApplicationWrapper>>() {
                });
    }

    private ApplicationWrapper loadApplication(final String applicationId) {
        return registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", applicationId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
    }

    private EndpointWrapper loadEndpoint(final String applicationId, final String endpointId) {
        return registry.target().path("api/application/{applicationId}/endpoint/{endpointId}")
                .resolveTemplate("applicationId", applicationId)
                .resolveTemplate("endpointId", endpointId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
    }


    private SearchPage getSearchPage() {
        return registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
    }

    private Collection<EntityLink> getLinks(final ApplicationWrapper applicationWrapper) {
        return ((Collection<Map<String, String>>)Map.class.cast(applicationWrapper.getSwagger().getVendorExtensions().get(TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY))
                .get(TribestreamOpenAPIExtension.LINKS)).stream()
                .map(m -> new EntityLink(m.get("rel"), m.get("href")))
                .collect(toList());
    }

    private String getLink(final ApplicationWrapper applicationWrapper, final String name) {
        return getLinks(applicationWrapper).stream()
                .filter(s -> name.equals(s.getRel()))
                .findFirst().get().getHref();
    }
}
