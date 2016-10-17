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
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.HistoryPage;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.Retry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.inject.Inject;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.stream.Stream;

import static java.lang.Math.abs;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;
import static org.tomitribe.tribestream.registryng.test.Registry.TESTUSER;

public class ApplicationHistoryResourceTest {
    @Inject
    @Tribe
    private ObjectMapper objectMapper;

    @Application
    private Registry registry;

    private Random random = new Random(System.currentTimeMillis());

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @After
    public void reset() {
        registry.restoreData();
    }

    @Test
    @Retry
    public void shouldLoadApplicationHistory() {
        // Given: A random application
        Collection<SearchResult> searchResults = getSearchPage().getResults();
        final String applicationId = searchResults.stream()
                .map(SearchResult::getApplicationId)
                .collect(toList())
                .get(abs(random.nextInt(searchResults.size())));

        Response applicationResponse = loadApplicationResponse(applicationId);
        final ApplicationWrapper wrapper = applicationResponse.readEntity(ApplicationWrapper.class);

        // When: I get the history link
        Response historyResponse = registry.client().target(getLink(wrapper, "history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(200, historyResponse.getStatus());

        final HistoryPage historyItems = historyResponse.readEntity(HistoryPage.class);

        // Then: I get at least one result
        assertNotNull(historyItems);
        assertTrue(historyItems.getItems().size() >= 1);

        // And: the first revision type is an ADD
        assertEquals("ADD", historyItems.getItems().get(historyItems.getItems().size() - 1).getRevisionType());

        // And: I get a link to the first and the last page
        assertNotNull(getLink(historyItems, "first"));
        assertNotNull(getLink(historyItems, "last"));
        assertNotNull(getLink(historyItems, "self"));
    }

    @Test
    public void shouldAddRevisionOnUpdate() {
        // Given: A random application with a history
        final Response applicationResponse = registry.withRetries(() -> {
            final Collection<SearchResult> searchResults = getSearchPage().getResults();
            final String applicationId = searchResults.stream()
                    .map(SearchResult::getApplicationId)
                    .collect(toList())
                    .get(abs(random.nextInt(searchResults.size())));
            return loadApplicationResponse(applicationId);
        });
        final ApplicationWrapper applicationWrapper;
        try {
            applicationWrapper = applicationResponse.readEntity(ApplicationWrapper.class);
        } catch (final ProcessingException pe) {
            throw new IllegalStateException("'" + applicationResponse.readEntity(String.class) + "'");
        }

        final HistoryPage historyItems = registry.client().target(getLink(applicationWrapper, "history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(HistoryPage.class);

        final String oldDescription = applicationWrapper.getSwagger().getInfo().getDescription();
        final String newDescription = UUID.randomUUID().toString();

        // When: I update the application
        applicationWrapper.getSwagger().getInfo().setDescription(newDescription);
        final Response updateResponse =
                registry.client().target(getLink(applicationWrapper, "self"))
                        .request(MediaType.APPLICATION_JSON_TYPE)
                        .put(entity(applicationWrapper, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(200, updateResponse.getStatus());

        final ApplicationWrapper updatedWrapper = updateResponse.readEntity(ApplicationWrapper.class);

        // Then: The history contains one additional item
        final Response newHistoryResponse = registry.client().target(getLink(updatedWrapper, "history"))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        final HistoryPage newHistoryItems = newHistoryResponse.readEntity(HistoryPage.class);

        assertEquals(historyItems.getItems().size() + 1, newHistoryItems.getItems().size());

        // And: Username is set to the current user and revisiontype is MOD
        assertEquals("MOD", newHistoryItems.getItems().get(0).getRevisionType());
        assertEquals(TESTUSER, newHistoryItems.getItems().get(0).getUsername());

        // And: The response with the new history contains links to the 2 different historic applications
        ApplicationWrapper currentApplicationWrapper = registry.client().target(getLink(newHistoryItems, "revision " + newHistoryItems.getItems().get(0).getRevisionId()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
        ApplicationWrapper oldApplicationWrapper = registry.client().target(getLink(newHistoryItems, "revision " + newHistoryItems.getItems().get(1).getRevisionId()))
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);

        assertEquals(newDescription, currentApplicationWrapper.getSwagger().getInfo().getDescription());
        assertEquals(oldDescription, oldApplicationWrapper.getSwagger().getInfo().getDescription());

    }

    private String getLink(final HistoryPage applicationWrapper, final String name) {
        return Stream.of(applicationWrapper.getLinks())
                .filter(s -> name.equals(s.getRel()))
                .findFirst().get().getHref();
    }

    private String getLink(final ApplicationWrapper applicationWrapper, final String name) {
        return ((Collection<Map<String, String>>)Map.class.cast(applicationWrapper.getSwagger().getVendorExtensions().get(TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY))
                .get(TribestreamOpenAPIExtension.LINKS)).stream()
                .filter(e -> e.get("rel").equals(name))
                .findFirst().get().get("href");
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
