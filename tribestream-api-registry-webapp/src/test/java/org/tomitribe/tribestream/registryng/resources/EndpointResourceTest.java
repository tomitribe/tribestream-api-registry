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
import io.swagger.models.Operation;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;

import javax.inject.Inject;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class EndpointResourceTest {
    private final Random random = new Random(System.currentTimeMillis());

    @Application
    private Registry registry;

    @Inject
    private SearchEngine engine;

    @Test
    public void shouldDeleteEndpoint() throws Exception {

        // Given: A random endpoint that I can also find via the search API

        final List<SearchResult> endpointSearchResults = new ArrayList<>(getSearchPage().getResults());

        SearchResult searchResult = endpointSearchResults.get(random.nextInt(endpointSearchResults.size()));

        String endpointUrl = searchResult.getLink();

        Response originalEndpointResponse = registry.client().target(endpointUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), originalEndpointResponse.getStatus());

        // When: I send a DELETE to the endpoint URL
        final Response response = registry.client().target(endpointUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildDelete()
                .invoke();

        // Then: I get a HTTP 200
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // And: I get a 404 when getting the endpoint
        assertEquals(
                Response.Status.NOT_FOUND.getStatusCode(),
                registry.client().target(endpointUrl)
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .buildGet()
                        .invoke()
                        .getStatus());

        // And: When I reload the application it doesn't contain the endpoint
        final ApplicationWrapper updatedApplication = loadApplication(searchResult.getApplicationId());
        final HttpMethod httpMethod = HttpMethod.valueOf(searchResult.getHttpMethod().toUpperCase());
        final String path = searchResult.getPath();
        assertTrue(
                updatedApplication.getSwagger().getPaths().get(path) == null
                        || updatedApplication.getSwagger().getPaths().get(path).getOperationMap().get(httpMethod) == null);

        // And: When I get the search page it does not contain this endpoint
        assertFalse(
                getSearchPage().getResults().stream()
                    .filter((SearchResult sr) -> endpointUrl.equals(sr.getLink()))
                    .findFirst()
                    .isPresent()
        );
    }

    @Test
    public void shouldUpdatePathAndVerbOfEndpoint() throws Exception {

        // Given: A random endpoint that I can also find via the search API
        final List<SearchResult> endpointSearchResults = new ArrayList<>(getSearchPage().getResults());

        final SearchResult searchResult = endpointSearchResults.get(random.nextInt(endpointSearchResults.size()));

        final String endpointUrl = searchResult.getLink();

        final ApplicationWrapper applicationWrapper = loadApplication(searchResult.getApplicationId());
        assertNotNull(applicationWrapper);

        final EndpointWrapper originalEndpoint = registry.client().target(endpointUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
        assertNotNull(originalEndpoint);

        // When: I update the path and verb of an endpoint
        final String newVerb = "patch";
        final String newPath = "/anotherpath";
        final String newSummary = UUID.randomUUID().toString();
        final Operation newOperation = new Operation();
        newOperation.setSummary(newSummary);
        final EndpointWrapper endpointWrapper = new EndpointWrapper(newVerb, newPath, newOperation);

        Response response = registry.client().target(endpointUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPut(Entity.entity(endpointWrapper, MediaType.APPLICATION_JSON_TYPE))
                .invoke();

        // Then: I get a HTTP 200
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

        // And: When I refetch the endpoint it has the new path and verb
        EndpointWrapper updatedEndpoint = registry.client().target(endpointUrl)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
        assertEquals(newVerb, updatedEndpoint.getHttpMethod());
        assertEquals(newPath, updatedEndpoint.getPath());
        assertEquals(newSummary, updatedEndpoint.getOperation().getSummary());

        // And: When I refetch the application it also contains the stub for the new endpoint
        ApplicationWrapper updatedApplication = loadApplication(searchResult.getApplicationId());
        assertNotNull(updatedApplication.getSwagger().getPaths().get(newPath));
        assertNotNull(updatedApplication.getSwagger().getPaths().get(newPath).getPatch());
        assertEquals(newSummary, updatedApplication.getSwagger().getPaths().get(newPath).getPatch().getSummary());

        engine.waitForWrites();

        // And: The searchpage also has the new properties
        Optional<SearchResult> updatedEndpointSearchResult = getSearchPage().getResults().stream()
                .filter((SearchResult sr) -> sr.getLink().equals(searchResult.getLink()))
                .findFirst();

        assertTrue(updatedEndpointSearchResult.isPresent());
        assertEquals(newPath, updatedEndpointSearchResult.get().getPath());
        assertEquals(newVerb, updatedEndpointSearchResult.get().getHttpMethod());
        assertEquals(newSummary, updatedEndpointSearchResult.get().getDescription());
    }

    @Test
    public void shouldCreateEndpoint() throws Exception {

        // Given: A new endpoint that I want to add to a random application
        final List<SearchResult> searchResults = new ArrayList<>(getSearchPage().getResults());
        final String applicationId = searchResults.get(0).getApplicationId();
        ApplicationWrapper applicationWrapper = loadApplication(applicationId);

        final String newPath = "/" + UUID.randomUUID().toString();
        final String newDescription = UUID.randomUUID().toString();
        final String newSummary = UUID.randomUUID().toString();
        final String newTag = UUID.randomUUID().toString();

        EndpointWrapper newEndpoint = new EndpointWrapper();
        newEndpoint.setHttpMethod("head");
        newEndpoint.setPath(newPath);
        final Operation operation = new Operation();
        operation.setDescription(newDescription);
        operation.setSummary(newSummary);
        operation.setTags(singletonList(newTag));
        newEndpoint.setOperation(operation);

        // When: I post the endpoint
        Response response = registry.target().path("api/application/{applicationId}/endpoint")
                .resolveTemplate("applicationId", applicationId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .buildPost(Entity.entity(newEndpoint, MediaType.APPLICATION_JSON_TYPE))
                .invoke();

        // Then: I get a HTTP 201
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        // And: When I refetch the application it has the new path and verb
        final ApplicationWrapper updatedApplication = loadApplication(applicationId);
        assertNotNull(updatedApplication.getSwagger().getPath(newPath));
        assertNotNull(updatedApplication.getSwagger().getPath(newPath).getHead());

        assertEquals(newSummary, updatedApplication.getSwagger().getPaths().get(newPath).getHead().getSummary());

        engine.waitForWrites();

        // And: The searchpage also has the new properties
        Optional<SearchResult> updatedEndpointSearchResult = getSearchPage().getResults().stream()
                .filter((SearchResult searchResult) -> "head".equalsIgnoreCase(searchResult.getHttpMethod()) && newPath.equals(searchResult.getPath()))
                .findFirst();

        assertTrue(updatedEndpointSearchResult.isPresent());
        assertEquals(newSummary, updatedEndpointSearchResult.get().getDescription());
    }


    private SearchPage getSearchPage() {
        return registry.target().path("api/registry")
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(SearchPage.class);
    }

    private ApplicationWrapper loadApplication(final String applicationId) {
        return registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", applicationId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
    }
}
