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
package org.tomitribe.tribestream.registry.resources;

import io.swagger.models.Operation;
import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registry.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registry.domain.EndpointWrapper;
import org.tomitribe.tribestream.registry.domain.TribestreamOpenAPIExtension;
import org.tomitribe.tribestream.registry.test.Registry;

import javax.ws.rs.NotFoundException;
import java.util.Map;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class HumanReadableResourceTest {
    @Application
    private Registry registry;

    @Test
    public void findEndpoint() {
        final EndpointWrapper wrapper = registry.target()
                .path("api/ui/endpoint/{application}/{verb}/{endpoint}")
                .resolveTemplate("application", "Swagger-Petstore")
                .resolveTemplate("verb", "GET")
                .resolveTemplate("endpoint", "pets/:id", false)
                .queryParam("version", "1.0.0")
                .request(APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
        assertPet(wrapper);
    }

    @Test // note that this would break if we add another version "as expected"
    public void findEndpointNoVersion() {
        final EndpointWrapper wrapper = registry.target()
                .path("api/ui/endpoint/{application}/{verb}/{endpoint}")
                .resolveTemplate("application", "Swagger-Petstore")
                .resolveTemplate("verb", "GET")
                .resolveTemplate("endpoint", "pets/:id", false)
                .request(APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
        assertPet(wrapper);
    }

    @Test(expected = NotFoundException.class)
    public void endpointNotFound() {
        registry.target()
                .path("api/ui/endpoint/{application}/{verb}/{endpoint}")
                .resolveTemplate("application", "Swagger-Petstore")
                .resolveTemplate("verb", "GET")
                .resolveTemplate("endpoint", "pets/missing", false)
                .queryParam("version", "1.0.0")
                .request(APPLICATION_JSON_TYPE)
                .get(EndpointWrapper.class);
    }

    @Test
    public void findApplication() {
        final ApplicationWrapper wrapper = registry.target()
                .path("api/ui/application/{application}")
                .resolveTemplate("application", "Swagger-Petstore")
                .queryParam("version", "1.0.0")
                .request(APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
        assertPet(wrapper);
    }

    @Test // note that this would break if we add another version "as expected"
    public void findApplicationNoVersion() {
        final ApplicationWrapper wrapper = registry.target()
                .path("api/ui/application/{application}")
                .resolveTemplate("application", "Swagger-Petstore")
                .request(APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
        assertPet(wrapper);
    }

    @Test(expected = NotFoundException.class)
    public void applicationNotFound() {
        registry.target()
                .path("api/ui/application/{application}")
                .resolveTemplate("application", "Swagger-Petstore_missing")
                .queryParam("version", "1.0.0")
                .request(APPLICATION_JSON_TYPE)
                .get(ApplicationWrapper.class);
    }

    private void assertPet(final ApplicationWrapper wrapper) {
        assertEquals("Swagger Petstore", wrapper.getSwagger().getInfo().getTitle());
        assertNotNull(wrapper.getSwagger());

        // ensure GUI will be able to navigate with the slugs we decided to use
        final Operation get = wrapper.getSwagger().getPath("/pets/{id}").getGet();
        assertNotNull(get.getVendorExtensions());
        assertNotNull(get.getVendorExtensions().get(TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY));
        assertEquals("pets/:id", Map.class.cast(get.getVendorExtensions()
                .get(TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY))
                .get(TribestreamOpenAPIExtension.HUMAN_READABLE_PATH));
    }

    private void assertPet(final EndpointWrapper wrapper) {
        assertEquals("get", wrapper.getHttpMethod());
        assertNotNull(wrapper.getApplicationId());
        assertNotNull(wrapper.getEndpointId());
        assertEquals("/pets/{id}", wrapper.getPath());
        assertNotNull(wrapper.getOperation());
    }
}
