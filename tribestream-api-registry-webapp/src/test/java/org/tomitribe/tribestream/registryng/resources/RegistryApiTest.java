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
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static javax.ws.rs.core.Response.Status.OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

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

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test
    public void testDeleteApplication() throws Exception {
        insertApplication("empty");

        final OpenApiDocument document = findApplicationByName("empty").orElseThrow(IllegalStateException::new);

        final Response response = registry.target()
                                          .path("api/application/{id}")
                                          .resolveTemplate("id", document.getId())
                                          .request()
                                          .delete();
        assertEquals(OK.getStatusCode(), response.getStatus());
        assertFalse(findApplicationByName("empty").isPresent());

        final JsonObject hits = findApplicationIndex("empty");
        assertEquals(0, hits.getInt("total"));
    }

    @Test
    public void testInsertAndFindEmptyApplication() throws Exception {
        insertApplication("empty");

        final SearchRequest searchRequest = new SearchRequest("empty", null, null, null, null, 0, 10);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals("empty", searchResult.getApplication().getApplicationName());
        assertTrue(searchResult.getEndpoints().isEmpty());
    }

    @Test
    public void testInsertAndAddEndpoint() throws Exception {
        testInsertAndFindEmptyApplication();

        final OpenApiDocument document = findApplicationByName("empty").orElseThrow(IllegalStateException::new);

        final EndpointWrapper endpoint = new EndpointWrapper();
        endpoint.setHttpMethod("POST");
        endpoint.setPath("/test");
        final Operation operation = new Operation();
        operation.setDescription("Description");
        operation.setSummary("Summary");
        endpoint.setOperation(operation);

        final Response response = registry.target().path("api/application/{applicationId}/endpoint")
                                          .resolveTemplate("applicationId", document.getId())
                                          .request(MediaType.APPLICATION_JSON_TYPE)
                                          .post(Entity.entity(endpoint, MediaType.APPLICATION_JSON_TYPE));
        final EndpointWrapper persistedEndpoint = response.readEntity(EndpointWrapper.class);
        assertNotNull(persistedEndpoint.getEndpointId());
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());

        final SearchRequest searchRequest = new SearchRequest("empty", null, null, null, null, 0, 10);
        final SearchPage searchPage = engine.search(searchRequest);
        assertNotNull(searchPage);

        final List<SearchResult> searchResults = searchPage.getResults();
        assertNotNull(searchResults);
        assertEquals(1, searchResults.size());

        final SearchResult searchResult = searchResults.get(0);
        assertEquals("empty", searchResult.getApplication().getApplicationName());
        assertFalse(searchResult.getEndpoints().isEmpty());
    }

    private void insertApplication(final String applicationName) {
        final ApplicationWrapper application = new ApplicationWrapper();
        application.setHumanReadableName(applicationName);
        application.setSwagger(
                new Swagger().info(new Info().title(applicationName).description(applicationName).version("1")));

        final Response response = registry.target()
                                          .path("api/application")
                                          .request(MediaType.APPLICATION_JSON_TYPE)
                                          .post(Entity.entity(application, MediaType.APPLICATION_JSON_TYPE));

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

    private Optional<OpenApiDocument> findApplicationByName(final String applicationName) {
        try {
            return Optional.of(em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_NAME, OpenApiDocument.class)
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
}
