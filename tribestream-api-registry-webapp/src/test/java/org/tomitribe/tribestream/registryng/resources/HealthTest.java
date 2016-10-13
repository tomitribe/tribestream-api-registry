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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.tomitribe.tribestream.registryng.service.monitoring.Validation;
import org.tomitribe.tribestream.registryng.service.monitoring.ValidationWrapper;
import org.tomitribe.tribestream.registryng.test.Registry;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import java.util.Collection;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;

@RunWith(TomEEEmbeddedSingleRunner.class)
public class HealthTest {
    @Application
    private Registry registry;

    @Test
    public void health() {
        final Collection<ValidationWrapper> validations = registry.target().path("api/health")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get(new GenericType<Collection<ValidationWrapper>>(){})
                .stream().sorted((a, b) -> a.getName().compareTo(b.getName()))
                .collect(toList());
        assertEquals(2, validations.size());
        validations.forEach(v -> assertEquals(Validation.State.OK, v.getValidation().getState()));
    }
}
