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
package org.tomitribe.tribestream.registryng.resources.enricher;

import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

import static javax.ws.rs.core.Link.fromUriBuilder;

@ApplicationScoped
public class Linker {
    public Link[] buildEndpointLinks(final UriInfo uriInfo, final String applicationId, final String endpointId) {
        return new Link[]{
                fromUriBuilder(uriInfo.getBaseUriBuilder()
                        .path("application/{applicationId}/endpoint/{endpointId}")
                        .resolveTemplate("applicationId", applicationId)
                        .resolveTemplate("endpointId", endpointId))
                        .rel("self")
                        .build(),
                fromUriBuilder(uriInfo.getBaseUriBuilder()
                        .path("history/application/{applicationId}/endpoint/{endpointId}")
                        .resolveTemplate("applicationId", applicationId)
                        .resolveTemplate("endpointId", endpointId))
                        .rel("history")
                        .build(),
                fromUriBuilder(uriInfo.getBaseUriBuilder()
                        .path("application/{applicationId}")
                        .resolveTemplate("applicationId", applicationId))
                        .rel("application")
                        .build()
        };
    }

    public Link[] buildApplicationLinks(final UriInfo uriInfo, final OpenApiDocument application) {
        final List<Link> result = new ArrayList<>(2 + application.getEndpoints().size());
        result.add(
                fromUriBuilder(uriInfo.getBaseUriBuilder()
                        .path("application/{applicationId}")
                        .resolveTemplate("applicationId", application.getId()))
                        .rel("self")
                        .build());
        result.add(
                fromUriBuilder(uriInfo.getBaseUriBuilder()
                        .path("history/application/{applicationId}")
                        .resolveTemplate("applicationId", application.getId()))
                        .rel("history")
                        .build());
        for (Endpoint endpoint : application.getEndpoints()) {
            result.add(
                    fromUriBuilder(uriInfo.getBaseUriBuilder()
                            .path("application/{applicationId}/endpoint/{endpointId}")
                            .resolveTemplate("applicationId", application.getId())
                            .resolveTemplate("endpointId", endpoint.getId()))
                            .rel(endpoint.getVerb().toUpperCase() + " " + endpoint.getPath())
                            .build());
        }

        return result.toArray(new Link[result.size()]);
    }
}
