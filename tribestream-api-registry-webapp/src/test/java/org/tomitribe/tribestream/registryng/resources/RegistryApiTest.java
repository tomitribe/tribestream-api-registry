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

import io.swagger.models.Info;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.search.SearchResult;
import org.tomitribe.tribestream.registryng.elasticsearch.ElasticsearchClient;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.search.SearchRequest;
import org.tomitribe.tribestream.registryng.test.Registry;

import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.net.URLEncoder;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static javax.ws.rs.core.Response.Status.CREATED;
import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.tomitribe.tribestream.registryng.entities.OpenApiDocument.Queries
        .FIND_BY_APPLICATIONID_WITH_ENDPOINTS;
import static org.tomitribe.tribestream.registryng.entities.OpenApiDocument.Queries.FIND_BY_NAME;

/**
 * Description.
 *
 * @author Roberto Cortez
 */
@RunWith(TomEEEmbeddedSingleRunner.class)
public class RegistryApiTest {
    @Application
    private Registry registry;

    @PersistenceContext
    private EntityManager em;

    @Inject
    private SearchEngine engine;
    @Inject
    private ElasticsearchClient elasticsearch;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.base", defaultValue = "http://localhost:9200")
    private String base;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.index", defaultValue = "tribestream-api-registry")
    private String index;

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test
    public void testEmpty() throws Exception {
        final Response delete = ClientBuilder.newBuilder().build().target(base).path(index).request().delete();
        assertEquals(OK.getStatusCode(), delete.getStatus());

        final SearchRequest searchRequest = new SearchRequest(null, null, null, null, null, 0, 0);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);
        assertEquals(0, searchPage.getTotal());
    }

    @Test
    public void testDeleteApplication() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);

        final OpenApiDocument document = findApplicationByName(name).orElseThrow(IllegalStateException::new);

        final Response response = registry.target()
                                          .path("api/application/{id}")
                                          .resolveTemplate("id", document.getId())
                                          .request()
                                          .delete();
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertFalse(findApplicationByName(name).isPresent());

        final JsonObject hits = findApplicationIndex(name);
        assertEquals(0, hits.getInt("total"));
    }

    @Test
    public void testInsertAndFindEmptyApplication() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);

        final SearchRequest searchRequest = new SearchRequest(name, null, null, null, null, 0, 10);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals(name, searchResult.getApplication().getApplicationName());
        assertTrue(searchResult.getEndpoints().isEmpty());
    }

    @Test
    public void testInsertAndAddEndpoint() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);

        final Response response = addEndpointToApplication(name, "endpoint");
        final EndpointWrapper persistedEndpoint = response.readEntity(EndpointWrapper.class);
        assertNotNull(persistedEndpoint.getEndpointId());

        final SearchPage searchPage = engine.search(new SearchRequest(name, null, null, null, null, 0, 10));
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals(name, searchResult.getApplication().getApplicationName());
        assertFalse(searchResult.getEndpoints().isEmpty());
    }

    @Test
    public void testRenameAndSearchEmptyApplication() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);

        final SearchPage searchPage = engine.search(new SearchRequest(name, null, null, null, null, 0, 10));
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals(name, searchResult.getApplication().getApplicationName());
        assertTrue(searchResult.getEndpoints().isEmpty());

        updateApplication(name, UUID.randomUUID().toString());
    }

    @Test
    public void testRenameAndSearchApplicationWithEndpoints() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);
        assertEquals(1, findApplicationIndex(name).getInt("total"));

        addEndpointToApplication(name, "endpoint");
        assertEquals(1, findApplicationIndex(name).getInt("total"));

        final String newName = UUID.randomUUID().toString();
        updateApplication(name, newName);

        final SearchPage searchPageOld = engine.search(new SearchRequest(name, null, null, null, null, 0, 10));
        assertNotNull(searchPageOld);
        final List<SearchResult> searchResultsOld = searchPageOld.getResults();
        assertNotNull(searchResultsOld);
        assertEquals(0, searchResultsOld.size());

        final SearchPage searchPageNew = engine.search(new SearchRequest(newName, null, null, null, null, 0, 10));
        assertNotNull(searchPageNew);
        final List<SearchResult> searchResultsNew = searchPageNew.getResults();
        assertNotNull(searchResultsNew);
        assertEquals(1, searchResultsNew.size());
        assertFalse(searchResultsNew.get(0).getEndpoints().isEmpty());
    }

    @Test
    public void testFindApplicationWithNoMoreEndpoints() throws Exception {
        final String name = UUID.randomUUID().toString();
        insertApplication(name);

        final Response response = addEndpointToApplication(name, "endpoint");
        final EndpointWrapper persistedEndpoint = response.readEntity(EndpointWrapper.class);
        assertNotNull(persistedEndpoint.getEndpointId());
        assertEquals(1, findApplicationIndex(name).getInt("total"));

        deleteAllEndpointsFromApplication(name);
        assertEquals(1, findApplicationIndex(name).getInt("total"));

        final SearchRequest searchRequest = new SearchRequest(name, null, null, null, null, 0, 10);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals(name, searchResult.getApplication().getApplicationName());
        assertTrue(searchResult.getEndpoints().isEmpty());
    }

    @Test
    public void testFindApplicationWithInvalidCharacters() throws Exception {
        assertEquals(5, findApplicationIndex("uber").getInt("total"));
        assertEquals(5, findApplicationIndex(URLEncoder.encode("uber!", "UTF-8")).getInt("total"));
        assertEquals(0, findApplicationIndex(URLEncoder.encode("!<>{}[]=@#$%&()^~_:;", "UTF-8")).getInt("total"));

        final SearchRequest searchRequest = new SearchRequest("uber!", null, null, null, null, 0, 10);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals(5, searchResult.getEndpoints().size());
    }

    private void insertApplication(final String applicationName) {
        final ApplicationWrapper application = new ApplicationWrapper();
        application.setHumanReadableName(applicationName);
        application.setSwagger(
                new Swagger().info(new Info().title(applicationName).description(applicationName).version("1")));

        final Response response = registry.target()
                                          .path("api/application")
                                          .request(APPLICATION_JSON_TYPE)
                                          .post(entity(application, APPLICATION_JSON_TYPE));
        assertEquals(CREATED.getStatusCode(), response.getStatus());

        final ApplicationWrapper persistedApplication = response.readEntity(ApplicationWrapper.class);
        assertNotNull(persistedApplication);

        final Optional<OpenApiDocument> persistedDocument = findApplicationByName(applicationName);
        assertTrue(persistedDocument.isPresent());
        assertNotNull(persistedDocument.get().getElasticsearchId());

        final JsonObject hits = findApplicationIndex(applicationName);
        assertEquals(1, hits.getInt("total"));

        final Optional<String> indexedName = hits.getJsonArray("hits")
                                                 .stream()
                                                 .map(json -> JsonObject.class.cast(json).getJsonObject("_source"))
                                                 .map(source -> source.getString("applicationName"))
                                                 .findFirst();
        assertTrue(indexedName.isPresent());
        assertEquals(applicationName, indexedName.get());
    }

    private void updateApplication(final String applicationName, final String newApplicationName) {
        final OpenApiDocument originalDocument =
                findApplicationByName(applicationName).orElseThrow(IllegalStateException::new);

        final ApplicationWrapper application = new ApplicationWrapper();
        application.setHumanReadableName(newApplicationName);
        application.setSwagger(
                new Swagger().info(new Info().title(newApplicationName).description(newApplicationName).version("2")));

        final Response response = registry.target()
                                          .path("api/application/{id}")
                                          .resolveTemplate("id", originalDocument.getId())
                                          .request(APPLICATION_JSON_TYPE)
                                          .put(entity(application, APPLICATION_JSON_TYPE));
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertFalse(findApplicationByName(applicationName).isPresent());
        assertTrue(findApplicationByName(newApplicationName).isPresent());
        assertEquals(0, findApplicationIndex(applicationName).getInt("total"));
        assertEquals(1, findApplicationIndex(newApplicationName).getInt("total"));
    }

    private Optional<OpenApiDocument> findApplicationByName(final String applicationName) {
        try {
            return Optional.of(em.createNamedQuery(FIND_BY_NAME, OpenApiDocument.class)
                                 .setParameter("name", applicationName)
                                 .getSingleResult());
        } catch (final NoResultException e) {
            return Optional.empty();
        }
    }

    private JsonObject findApplicationIndex(final String applicationName) {
        final JsonReader query = Json.createReader(
                new StringReader("{\n" +
                                 "  \"query\": {\n" +
                                 "    \"query_string\" : {\n" +
                                 "      \"query\" : \"" + applicationName + "\"\n" +
                                 "    }\n" +
                                 "  }\n" +
                                 "}"));
        return elasticsearch.search(query.readObject(), 0, 0).getJsonObject("hits");
    }

    private Response addEndpointToApplication(final String applicationName, final String path) {
        final OpenApiDocument document = findApplicationByName(applicationName).orElseThrow(IllegalStateException::new);

        final EndpointWrapper endpoint = new EndpointWrapper();
        endpoint.setHttpMethod("POST");
        endpoint.setPath("/" + path);
        final Operation operation = new Operation();
        operation.setDescription("Description");
        operation.setSummary("Summary");
        endpoint.setOperation(operation);

        final Response response = registry.target().path("api/application/{applicationId}/endpoint")
                                          .resolveTemplate("applicationId", document.getId())
                                          .request(APPLICATION_JSON_TYPE)
                                          .post(entity(endpoint, APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        return response;
    }

    private void deleteAllEndpointsFromApplication(final String applicationName) {
        final OpenApiDocument document =
                em.createNamedQuery(FIND_BY_APPLICATIONID_WITH_ENDPOINTS, OpenApiDocument.class)
                  .setParameter("applicationId",
                                findApplicationByName(applicationName).orElseThrow(IllegalStateException::new).getId())
                  .getSingleResult();
        assertNotNull(document.getEndpoints());
        assertFalse(document.getEndpoints().isEmpty());

        document.getEndpoints().stream().forEach(endpoint -> {
            final Response response = registry.target().path("api/application/{applicationId}/endpoint/{endpointId}")
                                              .resolveTemplate("applicationId", document.getId())
                                              .resolveTemplate("endpointId", endpoint.getId())
                                              .request()
                                              .delete();
            assertEquals(OK.getStatusCode(), response.getStatus());
        });
    }
}
