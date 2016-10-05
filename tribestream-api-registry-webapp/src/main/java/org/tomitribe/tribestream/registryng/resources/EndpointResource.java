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

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.resources.enricher.Linker;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("/application/{applicationId}/endpoint")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@NoArgsConstructor(force = true)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class EndpointResource {
    private final Repository repository;
    private final Linker linker;

    @GET
    @Path("/{endpointId}")
    public Response getEndpoint(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("endpointId") final String endpointId) {

        Endpoint endpoint = repository.findEndpointById(endpointId);

        if (endpoint == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!applicationId.equals(endpoint.getApplication().getId())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final EndpointWrapper endpointWrapper = new EndpointWrapper(
                applicationId, endpointId, endpoint.getHumanReadablePath(),
                endpoint.getVerb(), endpoint.getPath(), endpoint.getOperation());

        return Response.ok()
                .links(linker.buildEndpointLinks(uriInfo, applicationId, endpoint.getId()))
                .entity(endpointWrapper).build();
    }

    @DELETE
    @Path("/{endpointId}")
    public Response removeEndpoint(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("endpointId") final String endpointId) {

        Endpoint endpoint = repository.findEndpointById(endpointId);

        if (endpoint == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        if (!repository.deleteEndpoint(applicationId, endpointId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            return Response.status(Response.Status.OK).build();
        }
    }


    @POST
    @Path("/")
    public Response createService(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            final EndpointWrapper endpointWrapper) {

        final Operation operation = endpointWrapper.getOperation();

        final Endpoint endpoint = new Endpoint();
        endpoint.setVerb(endpointWrapper.getHttpMethod().toUpperCase());
        endpoint.setPath(endpointWrapper.getPath());
        endpoint.setOperation(endpointWrapper.getOperation());

        validate(endpoint);

        final Endpoint document = repository.insert(endpoint, applicationId);

        final Endpoint newDocument = repository.findEndpointById(document.getId());

        return Response.status(Response.Status.CREATED)
                .entity(new EndpointWrapper(
                        newDocument.getApplication().getId(), newDocument.getId(), newDocument.getHumanReadablePath(),
                        newDocument.getVerb(), newDocument.getPath(), newDocument.getOperation()))
                .links(linker.buildEndpointLinks(uriInfo, applicationId, newDocument.getId()))
                .build();

    }

    @PUT
    @Path("/{endpointId}")
    public Response updateService(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("endpointId") final String endpointId,
            final EndpointWrapper endpointWrapper) {

        final Endpoint oldEndpoint = repository.findEndpointById(endpointId);
        if (oldEndpoint == null || !applicationId.equals(oldEndpoint.getApplication().getId())) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        merge(oldEndpoint, endpointWrapper);

        validate(oldEndpoint);

        // TODO: Handle added/updated/removed paths

        repository.update(oldEndpoint);

        final Endpoint updatedDocument = repository.findEndpointById(endpointId);

        final EndpointWrapper newEndpointWrapper = new EndpointWrapper(
                applicationId, endpointId, updatedDocument.getHumanReadablePath(),
                updatedDocument.getVerb(), updatedDocument.getPath(), updatedDocument.getOperation());

        return Response.ok()
                .links(linker.buildEndpointLinks(uriInfo, applicationId, endpointId))
                .entity(newEndpointWrapper)
                .build();


    }

    private void validate(Endpoint endpoint) {
        try {
            HttpMethod.valueOf(endpoint.getVerb().toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new WebApplicationException(String.format("Verb %s is not supported!", endpoint.getVerb()), Response.Status.BAD_REQUEST);
        }

        // TODO: Add proper validation of path including placeholders
        // Whitespace is not allowed at leat
        if (endpoint.getPath().contains(" ")) {
            throw new WebApplicationException(String.format("Path %s is invalid!", endpoint.getPath()), Response.Status.BAD_REQUEST);
        }
    }

    private void merge(Endpoint target, EndpointWrapper source) {

        if (source.getPath() != null) {
            target.setPath(source.getPath());
        }
        if (source.getHttpMethod() != null) {
            target.setVerb(source.getHttpMethod());
        }
        merge(target.getOperation(), source.getOperation());
    }

    private void merge(Operation target, Operation source) {
        if (source.getSummary() != null) {
            target.setSummary(source.getSummary());
        }
        if (source.getDescription() != null) {
            target.setDescription(source.getDescription());
        }
        if (source.getOperationId() != null) {
            target.setOperationId(source.getOperationId());
        }
        if (source.getResponses() != null) {
            target.setResponses(source.getResponses());
        }
        if (source.getSchemes() != null) {
            target.setSchemes(source.getSchemes());
        }
        if (source.getConsumes() != null) {
            target.setConsumes(source.getConsumes());
        }
        if (source.getProduces() != null) {
            target.setProduces(source.getProduces());
        }
        if (source.getSecurity() != null) {
            target.setSecurity(source.getSecurity());
        }
        if (source.getParameters() != null) {
            target.setParameters(source.getParameters());
        }
        if (source.getResponses() != null) {
            target.setResponses(source.getResponses());
        }
        if (source.getSecurity() != null) {
            target.setSecurity(source.getSecurity());
        }
        if (source.getTags() != null) {
            target.setTags(source.getTags());
        }
        if (source.isDeprecated() != null) {
            target.setDeprecated(source.isDeprecated());
        }
        if (source.getExternalDocs() != null) {
            target.setExternalDocs(target.getExternalDocs());
        }
        if (source.getVendorExtensions() != null) {
            target.getVendorExtensions().clear();
            target.getVendorExtensions().putAll(source.getVendorExtensions());
        }
    }
}
