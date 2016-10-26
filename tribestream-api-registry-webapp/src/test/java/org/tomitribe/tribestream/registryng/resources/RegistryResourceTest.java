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
import io.swagger.models.Swagger;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.search.ApplicationSearchResult;
import org.tomitribe.tribestream.registryng.domain.search.SearchResult;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.Retry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static java.util.stream.Collectors.toSet;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.rules.RuleChain.outerRule;

public class RegistryResourceTest {
    @Application
    private Registry registry;

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @PersistenceContext
    private EntityManager em;

    @Inject
    @Tribe
    private ObjectMapper objectMapper;

    @Test
    @Retry
    public void drillDown() {
        final List<Endpoint> endpoints = em.createQuery("select e from Endpoint e", Endpoint.class).getResultList();
        final SearchPage root = registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(endpoints.toString() + "\n\n" + root.getResults(), endpoints.size(), root.getTotal());

        final SearchPage withTag = registry.target().path("api/registry")
                .queryParam("tag", "user")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(2, withTag.getTotal());

        final SearchPage withTagAndCategory = registry.target().path("api/registry")
                .queryParam("tag", "user")
                .queryParam("category", "partners")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(1, withTagAndCategory.getTotal());
    }

    @Test
    @Retry
    public void searchQuery() {
        final List<Endpoint> endpoints = em.createQuery("select e from Endpoint e", Endpoint.class).getResultList();

        // no query param
        final SearchPage root = registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(endpoints.toString() + "\n\n" + root.getResults(), endpoints.size(), root.getTotal());

        // wildcard = no filter
        final SearchPage wildcard = registry.target().path("api/registry")
                .queryParam("query", "*")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(endpoints.toString() + "\n\n" + root.getResults(), endpoints.size(), wildcard.getTotal());

        // custom query_string
        final SearchPage query = registry.target().path("api/registry")
                .queryParam("query", "Estimates")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(query.toString(), 2, query.getTotal());
    }

    @Test
    public void searchWithoutRestrictionsShouldShowEmptyApplications() throws Exception {
        final String applicationName = UUID.randomUUID().toString();
        // Given: A new empty service is created
        final String initialDocument = "{\n" +
                "  \"swagger\": \"2.0\",\n" +
                "  \"info\": {\n" +
                "    \"title\": \"" + applicationName + "\",\n" +
                "    \"version\": \"v2\"\n" +
                "  }\n" +
                "}";
        final Swagger createSwagger = objectMapper.readValue(initialDocument, Swagger.class);
        final ApplicationWrapper createRequest = new ApplicationWrapper(createSwagger, null);
        final Response newApplicationWrapperResponse = registry.target().path("api/application")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(createRequest, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(201, newApplicationWrapperResponse.getStatus());

        ApplicationWrapper newApplicationWrapper = newApplicationWrapperResponse.readEntity(ApplicationWrapper.class);

        // When: I fetch the full search page
        final SearchPage searchPage = registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);

        // Then it contains the empty application
        Set<String> allApplicationNames = searchPage.getResults().stream().map(SearchResult::getApplication).map(ApplicationSearchResult::getApplication).collect(toSet());
        assertThat(allApplicationNames, hasItem(applicationName));
        assertEquals(0, searchPage.getResults().stream().filter(searchResult -> searchResult.getApplication().getApplication().equals(applicationName)).findFirst().get().getEndpoints().size());
    }
}
