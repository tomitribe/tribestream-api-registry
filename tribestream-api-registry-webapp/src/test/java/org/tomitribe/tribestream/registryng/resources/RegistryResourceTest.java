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

import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.test.Registry;
import org.tomitribe.tribestream.registryng.test.retry.Retry;
import org.tomitribe.tribestream.registryng.test.retry.RetryRule;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.rules.RuleChain.outerRule;

@Retry
public class RegistryResourceTest {
    @Application
    private Registry registry;

    @Rule
    public final TestRule rule = outerRule(new TomEEEmbeddedSingleRunner.Rule(this)).around(new RetryRule(() -> registry));

    @PersistenceContext
    private EntityManager em;

    @Test
    public void drillDown() {
        final int max = em.createQuery("select count(e) from Endpoint e", Number.class).getSingleResult().intValue();
        final SearchPage root = registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(max, root.getTotal());

        final SearchPage withTag = registry.target().path("api/registry")
                .queryParam("tag", "partners")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(2, withTag.getTotal());

        final SearchPage withTagAndCategory = registry.target().path("api/registry")
                .queryParam("tag", "partners")
                .queryParam("category", "partners")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(1, withTagAndCategory.getTotal());
    }

    @Test
    public void searchQuery() {
        final int max = em.createQuery("select count(e) from Endpoint e", Number.class).getSingleResult().intValue();

        // no query param
        final SearchPage root = registry.target().path("api/registry")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(max, root.getTotal());

        // wildcard = no filter
        final SearchPage wildcard = registry.target().path("api/registry")
                .queryParam("query", "*")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(max, wildcard.getTotal());

        // custom query_string
        final SearchPage query = registry.target().path("api/registry")
                .queryParam("query", "partners")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(SearchPage.class);
        assertEquals(query.toString(), 2, query.getTotal());
    }
}
