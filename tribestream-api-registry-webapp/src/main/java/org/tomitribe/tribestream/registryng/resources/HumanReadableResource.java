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

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.resources.enricher.Linker;
import org.tomitribe.tribestream.registryng.resources.processor.ApplicationProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import static java.util.Optional.ofNullable;

@Path("ui")
@Produces(MediaType.APPLICATION_JSON)
@Transactional
@ApplicationScoped
@NoArgsConstructor(force = true)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class HumanReadableResource {
    private final Repository repository;
    private final Linker linker;
    private final ApplicationProcessor processor;

    @GET
    @Path("endpoint/{applicationName}/{method}/{path: .+}")
    public Response getEndpointHumanReadablePath(@QueryParam("version") final String version,
                                                 @PathParam("applicationName") final String appName,
                                                 @PathParam("method") final String method,
                                                 @PathParam("path") final String path,
                                                 @Context final UriInfo info) {
        return ofNullable(repository.findEndpointFromHumanReadableMeta(appName, method, path, version))
                .map(e -> new EndpointWrapper(e.getApplication().getId(), e.getId(), e.getHumanReadablePath(), e.getVerb(), e.getPath(), e.getOperation()))
                .map(w -> Response.ok(w).links(linker.buildEndpointLinks(info, w.getApplicationId(), w.getEndpointId())).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }

    @GET
    @Path("application/{applicationName}")
    public Response getApplicationHumanReadablePath(@QueryParam("version") final String version,
                                                    @PathParam("applicationName") final String appName,
                                                    @Context final UriInfo info) {
        return ofNullable(repository.findApplicationFromHumanReadableMetadata(appName, version))
                .map(d -> Response.ok(processor.toWrapper(d)).links(linker.buildApplicationLinks(info, d)).build())
                .orElseGet(() -> Response.status(Response.Status.NOT_FOUND).build());
    }
}
