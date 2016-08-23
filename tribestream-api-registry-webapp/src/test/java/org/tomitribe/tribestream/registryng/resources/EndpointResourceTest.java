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

import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.service.serialization.CustomJacksonJaxbJsonProvider;
import com.tomitribe.tribestream.test.registryng.category.Embedded;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.openejb.junit.ApplicationComposerRule;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@Category(Embedded.class)
public class EndpointResourceTest {

    @ClassRule
    public final static ApplicationComposerRule app = new ApplicationComposerRule(new Application());

    @Test
    public void shouldLoadAllApplications() throws Exception {

        SearchPage allEndpoints = loadDefaultSearchPage();

        assertEquals(4, allEndpoints.getApplications().size());
        final Collection<SearchResult> results = allEndpoints.getResults();

        assertTrue(results.size() > 0);

        for (SearchResult result: results) {
            final String expectedLink = "http://localhost:" + getPort() + "/openejb/api/endpoint/" + result.getEndpointId();
            final EndpointWrapper endpointWrapper = getClient().target(expectedLink)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(EndpointWrapper.class);

            assertEquals(expectedLink, endpointWrapper.getLinks().get("self"));
            assertEquals("http://localhost:" + getPort() + "/openejb/api/application/" + result.getDeployableId(), endpointWrapper.getLinks().get("application"));
        }
    }

    private SearchPage loadDefaultSearchPage() {
        WebClient webClient = WebClient.create("http://localhost:" + getPort(), Arrays.asList(new CustomJacksonJaxbJsonProvider()))
            .accept(MediaType.APPLICATION_JSON_TYPE);

        return webClient.path("openejb/api/registry").get(SearchPage.class);
    }

    private Application getApp() {
        return app.getInstance(Application.class);
    }

    private int getPort() {
        return getApp().getPort();
    }

    public Client getClient() {
        return getApp().getClient();
    }

}
