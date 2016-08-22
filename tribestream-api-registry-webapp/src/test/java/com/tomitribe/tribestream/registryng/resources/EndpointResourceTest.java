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
package com.tomitribe.tribestream.registryng.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tomitribe.tribestream.registryng.bootstrap.Bootstrap;
import com.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import com.tomitribe.tribestream.registryng.domain.SearchPage;
import com.tomitribe.tribestream.registryng.domain.SearchResult;
import com.tomitribe.tribestream.registryng.repository.Repository;
import com.tomitribe.tribestream.registryng.service.search.SearchEngine;
import com.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import com.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import com.tomitribe.tribestream.registryng.webapp.RegistryNgApplication;
import com.tomitribe.tribestream.test.registryng.category.Embedded;
import com.tomitribe.tribestream.test.registryng.util.DefaultContainer;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.junit.ApplicationComposerRule;
import org.apache.openejb.testing.Classes;
import org.apache.openejb.testing.EnableServices;
import org.apache.openejb.testing.JaxrsProviders;
import org.apache.openejb.testing.Module;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ejb.EJB;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(Embedded.class)
@EnableServices("jaxrs")
public class EndpointResourceTest extends AbstractResourceTest {

    private final DefaultContainer container = new DefaultContainer(true);

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper objectMapper;

    @Rule
    public final ApplicationComposerRule app = new ApplicationComposerRule(this, container);

    @Module
    @JaxrsProviders(CustomJacksonJaxbJsonProvider.class)
    @Classes(cdi = true, value = {
        Repository.class, SwaggerJsonMapperProducer.class,
        Bootstrap.class, SearchEngine.class,
        RegistryResource.class, EndpointResource.class,
        RegistryNgApplication.class
    })
    public WebApp war() throws Exception {
        return new WebApp();
    }

    @EJB
    private Repository repository;

    @Test
    public void shouldLoadAllApplications() throws Exception {

        SearchPage allEndpoints = loadDefaultSearchPage();

        assertEquals(2, allEndpoints.getApplications().size());
        final Collection<SearchResult> results = allEndpoints.getResults();

        assertTrue(results.size() > 0);

        for (SearchResult result: results) {
            final String expectedLink = "http://localhost:" + container.getPort() + "/openejb/api/endpoint/" + result.getEndpointId();
            final EndpointWrapper endpointWrapper = getClient().target(expectedLink)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(EndpointWrapper.class);

            assertEquals(expectedLink, endpointWrapper.getLinks().get("self"));
            assertEquals("http://localhost:" + container.getPort() + "/openejb/api/application/" + result.getDeployableId(), endpointWrapper.getLinks().get("application"));
        }
    }

    private SearchPage loadDefaultSearchPage() {
        WebClient webClient = WebClient.create("http://localhost:" + container.getPort(), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE);

        return webClient.path("openejb/api/registry").get(SearchPage.class);
    }

}
