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
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.HistoryItem;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.Retry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.rules.RuleChain.outerRule;
import static org.tomitribe.tribestream.registryng.test.Registry.TESTUSER;

public class EndpointHistoryResourceTest {
    @Inject
    @Tribe
    private ObjectMapper objectMapper;

    @Application
    private Registry registry;

    @PersistenceContext
    private EntityManager em;

    private Random random = new Random(System.currentTimeMillis());

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test
    @Retry
    public void shouldLoadEndpointHistory() {
        // Given: A random application
        Collection<SearchResult> searchResults = getSearchPage().getResults();
        final SearchResult searchResult = new ArrayList<>(searchResults).get(random.nextInt(searchResults.size()));

        final String applicationId = searchResult.getApplicationId();
        final String endpointId = searchResult.getEndpointId();

        Response endpointResponse = loadEndpointResponse(applicationId, endpointId);

        assertNotNull(endpointResponse.getLink("history"));
        // When: I get the history link
        Response historyResponse = registry.client().target(endpointResponse.getLink("history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(Response.Status.OK.getStatusCode(), historyResponse.getStatus());

        List<HistoryItem> historyItems = historyResponse.readEntity(new GenericType<List<HistoryItem>>() {
        });

        // Then: I get at least one result
        assertNotNull(historyItems);
        assertTrue(historyItems.size() >= 1);

        // And: the first revision type is an ADD
        assertEquals("ADD", historyItems.get(historyItems.size() - 1).getRevisionType());

        // And: I get a link to the first and the last page
        assertNotNull(historyResponse.getLink("first"));
        assertNotNull(historyResponse.getLink("last"));
        assertNotNull(historyResponse.getLink("self"));
        historyResponse.getLinks().forEach(System.out::println);
    }

    @Test
    public void shouldAddRevisionOnUpdate() {
        // Given: A random application with a history
        final Response endpointResponse = registry.withRetries(() -> {
            Collection<SearchResult> searchResults = getSearchPage().getResults();
            final SearchResult searchResult = new ArrayList<>(searchResults).get(random.nextInt(searchResults.size()));

            final String applicationId = searchResult.getApplicationId();
            final String endpointId = searchResult.getEndpointId();

            return loadEndpointResponse(applicationId, endpointId);
        });

        final EndpointWrapper endpointWrapper = endpointResponse.readEntity(EndpointWrapper.class);

        final List<HistoryItem> historyItems = registry.client().target(endpointResponse.getLink("history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<HistoryItem>>() {
                });

        final String oldDescription = endpointWrapper.getOperation().getDescription();
        final String newDescription = UUID.randomUUID().toString();

        // When: I update the application
        endpointWrapper.getOperation().setDescription(newDescription);
        final Response updateResponse =
                registry.client().target(endpointResponse.getLink("self"))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .put(entity(endpointWrapper, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(Response.Status.OK.getStatusCode(), updateResponse.getStatus());

        // Then: The history contains one additional item
        registry.withRetries(() -> {
            final Response newHistoryResponse = registry.client().target(updateResponse.getLink("history"))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get();

            final List<HistoryItem> newHistoryItems = newHistoryResponse.readEntity(new GenericType<List<HistoryItem>>() {
            });

            assertEquals(historyItems.size() + 1, newHistoryItems.size());

            // And: Username is set to the current user and revisiontype is MOD
            assertEquals("MOD", newHistoryItems.get(0).getRevisionType());
            assertEquals(TESTUSER, newHistoryItems.get(0).getUsername());

            // And: The response with the new history contains links to the 2 different historic applications
            newHistoryResponse.getLinks().forEach(System.out::println);
            EndpointWrapper currentEndpointWrapper = registry.client().target(newHistoryResponse.getLink("revision " + newHistoryItems.get(0).getRevisionId()))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(EndpointWrapper.class);
            EndpointWrapper oldEndpointWrapper = registry.client().target(newHistoryResponse.getLink("revision " + newHistoryItems.get(1).getRevisionId()))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(EndpointWrapper.class);

            assertEquals(newDescription, currentEndpointWrapper.getOperation().getDescription());
            assertEquals(oldDescription, oldEndpointWrapper.getOperation().getDescription());
        });
    }

    @Test
    public void loadRevisionHasPayload() {
        // Given: A random application
        final SearchResult searchResult = registry.withRetries(() -> {
            Collection<SearchResult> searchResults = getSearchPage().getResults();
            final SearchResult result = new ArrayList<>(searchResults).get(random.nextInt(searchResults.size()));
            assertNotNull("endpoint exists", em.find(Endpoint.class, result.getEndpointId()));
            return result;
        });

        final String applicationId = searchResult.getApplicationId();
        final String endpointId = searchResult.getEndpointId();

        final AuditReader auditReader = AuditReaderFactory.get(em);
        final Number revision = auditReader.getRevisions(Endpoint.class, endpointId).iterator().next();

        final String json = registry.target()
                .path("api/history/application/{applicationId}/endpoint/{endpointId}/{revision}")
                .resolveTemplate("applicationId", applicationId)
                .resolveTemplate("endpointId", endpointId)
                .resolveTemplate("revision", revision.intValue())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class)
                .getJson();

        assertNotNull(json);
        assertFalse(json.trim().isEmpty());
        try { // valid it is json
            new ObjectMapper().readTree(json);
        } catch (final IOException e) {
            fail(e.getMessage());
        }
    }

    private Response loadEndpointResponse(final String applicationId, final String endpointId) {
        return registry.target().path("api/application/{applicationId}/endpoint/{endpointId}")
                .resolveTemplate("applicationId", applicationId)
                .resolveTemplate("endpointId", endpointId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
    }

    private SearchPage getSearchPage() {
        return registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
    }
}
