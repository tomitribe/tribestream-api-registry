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
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/endpoint")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class EndpointResource {

    private final Repository repository;


    @Inject
    public EndpointResource(
        Repository repository,
        @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME) ObjectMapper jsonMapper) {
        this.repository = repository;
    }

    public EndpointResource() {
        this(null, null);
    }

    @GET
    @Path("/{endpointId}")
    public Response getEndpoint(@Context UriInfo uriInfo,
                                @PathParam("endpointId") final String endpointId) {
        final Endpoint endpoint = repository.findEndpointById(endpointId);
        if (endpoint == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        EndpointWrapper endpointWrapper = new EndpointWrapper(endpoint.getVerb().toLowerCase(), endpoint.getPath(), endpoint.getOperation());
        endpointWrapper.addLink("self", uriInfo.getBaseUriBuilder().path("endpoint").path(endpoint.getId()).build());
        endpointWrapper.addLink("application", uriInfo.getBaseUriBuilder().path("application").path(endpoint.getApplication().getId()).build());

        return Response.ok(endpointWrapper).build();
    }

}
