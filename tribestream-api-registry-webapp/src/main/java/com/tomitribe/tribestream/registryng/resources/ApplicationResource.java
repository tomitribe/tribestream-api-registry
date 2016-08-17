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
import com.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import com.tomitribe.tribestream.registryng.domain.EndpointWrapper;
import com.tomitribe.tribestream.registryng.entities.Endpoint;
import com.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import com.tomitribe.tribestream.registryng.repository.Repository;
import com.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Path("/application")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationResource {

    private final Repository repository;


    @Inject
    public ApplicationResource(
        Repository repository,
        @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME) ObjectMapper jsonMapper) {
        this.repository = repository;
    }

    public ApplicationResource() {
        this(null, null);
    }

    @GET
    @Path("/")
    public List<ApplicationWrapper> getAllApplications(@Context UriInfo uriInfo) {
        final List<OpenApiDocument> applications = repository.findAllApplicationsWithEndpoints();

        final Set<String> ids = new HashSet<>();
        final List<ApplicationWrapper> uniqueResults = new ArrayList<>();

        for (OpenApiDocument application : applications) {
            final Swagger swagger = shrinkSwagger(mergeSwagger(application.getSwagger(), application.getEndpoints()));
            ApplicationWrapper applicationWrapper = new ApplicationWrapper(swagger);
            applicationWrapper.addLink("self", uriInfo.getBaseUriBuilder().path("application").path(application.getId()).build());
            uniqueResults.add(applicationWrapper);
        }

        return uniqueResults;
    }

    @GET
    @Path("/{applicationId}")
    public Response getApplication(
        @Context UriInfo uriInfo,
        @PathParam("applicationId") final String applicationId) {

        final OpenApiDocument application = repository.findByApplicationIdWithEndpoints(applicationId);
        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        final Swagger reducedSwagger = shrinkSwagger(mergeSwagger(application.getSwagger(), application.getEndpoints()));

        ApplicationWrapper applicationWrapper = new ApplicationWrapper(reducedSwagger);
        applicationWrapper.addLink("self", uriInfo.getBaseUriBuilder().path("application").path(application.getId()).build());

        return Response.ok(applicationWrapper).build();
    }


    @GET
    @Path("/{applicationId}/{verb}/{path:.*}")
    public Response getEndpoint(
        @Context UriInfo uriInfo,
        @PathParam("applicationId") final String applicationId,
        @PathParam("verb") final String verb,
        @PathParam("path") final String pathWithoutLeadingSlash) {

        final String path = "/" + pathWithoutLeadingSlash;

        Endpoint endpoint = repository.findEndpoint(applicationId, verb, path);
        if (endpoint == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
        EndpointWrapper endpointWrapper = new EndpointWrapper(verb, path, endpoint.getOperation());
        endpointWrapper.addLink("self", uriInfo.getBaseUriBuilder().path("endpoint").path(endpoint.getId()).build());
        endpointWrapper.addLink("application", uriInfo.getBaseUriBuilder().path("application").path(applicationId).build());

        return Response.ok(endpointWrapper).build();
    }

    // For root resources
    @GET
    @Path("/{applicationId}/{verb}")
    public Response getEndpoint(
        @Context UriInfo uriInfo,
        @PathParam("applicationId") final String applicationId,
        @PathParam("verb") final String verb) {
        return getEndpoint(uriInfo, applicationId, verb, "");
    }

    @POST
    @Path("/")
    public Response createService(
        @Context UriInfo uriInfo,
        ApplicationWrapper application) {

        final Swagger swagger = application.getSwagger();

        final OpenApiDocument document = repository.insert(swagger);

        final OpenApiDocument newDocument = repository.findByApplicationIdWithEndpoints(document.getId());

        final ApplicationWrapper applicationWrapper = new ApplicationWrapper(shrinkSwagger(mergeSwagger(newDocument.getSwagger(), newDocument.getEndpoints())));
        applicationWrapper.addLink("self", uriInfo.getBaseUriBuilder().path("application").path(document.getId()).build());

        return Response.status(201).entity(applicationWrapper).build();
    }

    private Swagger mergeSwagger(final Swagger swagger, final Collection<Endpoint> endpoints) {
        final Swagger result = Repository.createShallowCopy(swagger);
        final HashMap<String, io.swagger.models.Path> newPaths = new HashMap<>();

        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                io.swagger.models.Path newPath = newPaths.get(endpoint.getPath());
                if (newPath == null) {
                    newPath = new io.swagger.models.Path();
                    newPaths.put(endpoint.getPath(), newPath);
                }
                newPath.set(endpoint.getVerb().toLowerCase(), endpoint.getOperation());
            }
        }
        result.setPaths(newPaths);
        return result;
    }


    private Swagger shrinkSwagger(final Swagger swagger) {
        Swagger applicationClone = Repository.createShallowCopy(swagger);

        Map<String, io.swagger.models.Path> paths = applicationClone.getPaths();
        Map<String, io.swagger.models.Path> shrunkPaths = new HashMap<>();

        for (Map.Entry<String, io.swagger.models.Path> pathEntry : paths.entrySet()) {
            io.swagger.models.Path shrunkPath = new io.swagger.models.Path();
            shrunkPaths.put(pathEntry.getKey(), shrunkPath);
            for (Map.Entry<HttpMethod, Operation> httpMethodOperationEntry : pathEntry.getValue().getOperationMap().entrySet()) {
                Operation shrunkOperation = new Operation();
                shrunkOperation.setDescription(httpMethodOperationEntry.getValue().getDescription());
                shrunkOperation.setSummary(httpMethodOperationEntry.getValue().getSummary());
                shrunkPath.set(httpMethodOperationEntry.getKey().name().toLowerCase(), shrunkOperation);
            }
        }

        applicationClone.setPaths(shrunkPaths);
        return applicationClone;
    }

}
