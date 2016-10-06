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
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.HistoryItem;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;

import javax.inject.Inject;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.tomitribe.tribestream.registryng.resources.Registry.TESTUSER;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class ApplicationHistoryResourceTest {
    @Inject
    @Tribe
    private ObjectMapper objectMapper;

    @Application
    private Registry registry;

    private Random random = new Random(System.currentTimeMillis());

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test
    public void shouldLoadApplicationHistory() {
        // Given: A random application
        Collection<SearchResult> searchResults = getSearchPage().getResults();
        final String applicationId = searchResults.stream()
                .map(SearchResult::getApplicationId)
                .collect(toList())
                .get(abs(random.nextInt(searchResults.size())));

        Response applicationResponse= loadApplicationResponse(applicationId);

        // When: I get the history link
        Response historyResponse = registry.client().target(applicationResponse.getLink("history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(200, historyResponse.getStatus());

        List<HistoryItem> historyItems = historyResponse.readEntity(new GenericType<List<HistoryItem>>() {});

        // Then: I get at least one result
        assertNotNull(historyItems);
        assertTrue(historyItems.size() >= 1);

        // And: the first revision type is an ADD
        assertEquals("ADD", historyItems.get(historyItems.size() - 1).getRevisionType());

        // And: I get a link to the first and the last page
        assertNotNull(historyResponse.getLink("first"));
        assertNotNull(historyResponse.getLink("last"));
        assertNotNull(historyResponse.getLink("self"));
    }

    @Test
    public void shouldAddRevisionOnUpdate() {
        // Given: A random application with a history
        final Collection<SearchResult> searchResults = getSearchPage().getResults();
        final String applicationId = searchResults.stream()
                .map(SearchResult::getApplicationId)
                .collect(toList())
                .get(abs(random.nextInt(searchResults.size())));

        final Response applicationResponse = loadApplicationResponse(applicationId);
        final ApplicationWrapper applicationWrapper = applicationResponse.readEntity(ApplicationWrapper.class);

        final List<HistoryItem> historyItems = registry.client().target(applicationResponse.getLink("history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<List<HistoryItem>>() {});

        final String oldDescription = applicationWrapper.getSwagger().getInfo().getDescription();
        final String newDescription = UUID.randomUUID().toString();

        // When: I update the application
        applicationWrapper.getSwagger().getInfo().setDescription(newDescription);
        final Response updateResponse =
                registry.client().target(applicationResponse.getLink("self"))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .put(entity(applicationWrapper, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, updateResponse.getStatus());

        // Then: The history contains one additional item
        final Response newHistoryResponse = registry.client().target(updateResponse.getLink("history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        final List<HistoryItem> newHistoryItems = newHistoryResponse.readEntity(new GenericType<List<HistoryItem>>() {});

        assertEquals(historyItems.size() + 1, newHistoryItems.size());

        newHistoryItems.forEach(System.out::println);

        // And: Username is set to the current user and revisiontype is MOD
        assertEquals("MOD", newHistoryItems.get(0).getRevisionType());
        assertEquals(TESTUSER, newHistoryItems.get(0).getUsername());

        // And: The response with the new history contains links to the 2 different historic applications
        ApplicationWrapper currentApplicationWrapper = registry.client().target(newHistoryResponse.getLink("revision " + newHistoryItems.get(0).getRevisionId()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
        ApplicationWrapper oldApplicationWrapper = registry.client().target(newHistoryResponse.getLink("revision " + newHistoryItems.get(1).getRevisionId()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);

        assertEquals(newDescription, currentApplicationWrapper.getSwagger().getInfo().getDescription());
        assertEquals(oldDescription, oldApplicationWrapper.getSwagger().getInfo().getDescription());

    }

    private Response loadApplicationResponse(final String applicationId) {
        return registry.target().path("api/application/{applicationId}")
                .resolveTemplate("applicationId", applicationId)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();
    }

    private SearchPage getSearchPage() {
        return registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
    }
}
