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

import io.swagger.models.Swagger;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;

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
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.tomitribe.tribestream.registryng.resources.util.ApplicationWrapperUtil.mergeSwagger;
import static org.tomitribe.tribestream.registryng.resources.util.ApplicationWrapperUtil.shrinkSwagger;

@Path("/application")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class ApplicationResource {

    private final Repository repository;

    private final SearchEngine searchEngine;

    @Inject
    public ApplicationResource(
        Repository repository,
        SearchEngine searchEngine) {
        this.repository = repository;
        this.searchEngine = searchEngine;
    }

    public ApplicationResource() {
        this(null, null);
    }

    @GET
    @Path("/")
    public Response getAllApplications(@Context UriInfo uriInfo) {
        final List<OpenApiDocument> applications = repository.findAllApplicationsWithEndpoints();

        final Set<String> ids = new HashSet<>();
        final List<ApplicationWrapper> uniqueResults = new ArrayList<>();

        for (OpenApiDocument application : applications) {
            final Swagger swagger = shrinkSwagger(mergeSwagger(application.getSwagger(), application.getEndpoints()));
            ApplicationWrapper applicationWrapper = new ApplicationWrapper(swagger);
            uniqueResults.add(applicationWrapper);
        }

        return Response.status(Response.Status.OK).entity(uniqueResults).build();
    }

    private Link[] buildLinks(UriInfo uriInfo, OpenApiDocument application) {
        List<Link> result = new ArrayList<>();

        result.add(
                Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path("application/{applicationId}").resolveTemplate("applicationId", application.getId()))
                        .rel("self")
                        .build());
        result.add(
                Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path("history/application/{applicationId}").resolveTemplate("applicationId", application.getId()))
                        .rel("history")
                        .build());
        for (Endpoint endpoint : application.getEndpoints()) {
            result.add(
                    Link.fromUriBuilder(uriInfo.getBaseUriBuilder().path("application/{applicationId}/endpoint/{endpointId}")
                            .resolveTemplate("applicationId", application.getId())
                            .resolveTemplate("endpointId", endpoint.getId()))
                            .rel(endpoint.getVerb() + " " + endpoint.getPath())
                            .build());
        }

        return result.toArray(new Link[result.size()]);
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

        return Response.ok(applicationWrapper).links(buildLinks(uriInfo, application)).build();
    }


    @POST
    @Path("/")
    public Response createService(
        @Context UriInfo uriInfo,
        ApplicationWrapper application) {

        final Swagger swagger = application.getSwagger();

        validate(swagger);

        final OpenApiDocument document = repository.insert(swagger);

        final OpenApiDocument newDocument = repository.findByApplicationIdWithEndpoints(document.getId());

        final ApplicationWrapper applicationWrapper = new ApplicationWrapper(shrinkSwagger(mergeSwagger(newDocument.getSwagger(), newDocument.getEndpoints())));

        searchEngine.doReindex();

        return Response.status(Response.Status.CREATED)
                .entity(applicationWrapper)
                .links(buildLinks(uriInfo, newDocument))
                .build();
    }

    private void validate(Swagger swagger) {
        if (swagger == null) {
            throw new WebApplicationException("Swagger document is null!", Response.Status.BAD_REQUEST);
        }
        if (!"2.0".equals(swagger.getSwagger())) {
            throw new WebApplicationException("Unsupported swagger version!", Response.Status.BAD_REQUEST);
        }
        if (swagger.getInfo() == null) {
            throw new WebApplicationException("Swagger document has no info property!", Response.Status.BAD_REQUEST);
        }
        if (swagger.getInfo().getTitle() == null) {
            throw new WebApplicationException("Swagger document has no title!", Response.Status.BAD_REQUEST);
        }
        if (swagger.getInfo().getTitle() == null) {
            throw new WebApplicationException("Swagger document has no version!", Response.Status.BAD_REQUEST);
        }
    }

    @PUT
    @Path("/{id}")
    public Response updateService(
            @Context UriInfo uriInfo,
            @PathParam("id") final String applicationId,
            ApplicationWrapper application) {

        final Swagger swagger = application.getSwagger();

        final OpenApiDocument oldDocument = repository.findByApplicationIdWithEndpoints(applicationId);

        if (oldDocument == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        merge(oldDocument.getSwagger(), swagger);

        validate(oldDocument.getSwagger());

        // TODO: Handle added/updated/removed paths

        repository.update(oldDocument);

        final OpenApiDocument updatedDocument = repository.findByApplicationIdWithEndpoints(applicationId);

        final ApplicationWrapper applicationWrapper = new ApplicationWrapper(shrinkSwagger(updatedDocument.getSwagger()));

        searchEngine.doReindex();

        return Response.status(Response.Status.OK).links(buildLinks(uriInfo, updatedDocument)).entity(applicationWrapper).build();
    }


    @DELETE
    @Path("/{id}")
    public Response deleteService(
            @Context UriInfo uriInfo,
            @PathParam("id") final String applicationId) {

        if (!repository.deleteApplication(applicationId)) {
            return Response.status(Response.Status.NOT_FOUND).build();
        } else {
            searchEngine.doReindex();
            return Response.status(Response.Status.OK).build();
        }
    }

    private void merge(Swagger target, Swagger source) {

        if (source.getSwagger() != null) {
            target.setSwagger(source.getSwagger());
        }
        if (source.getInfo() != null) {
            target.setInfo(source.getInfo());
        }
        if (source.getHost() != null) {
            target.setHost(source.getHost());
        }
        if (source.getBasePath() != null) {
            target.setBasePath(source.getBasePath());
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
        if (source.getDefinitions() != null) {
            target.setDefinitions(source.getDefinitions());
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
        if (source.getVendorExtensions() != null) {
            target.getVendorExtensions().clear();
            target.getVendorExtensions().putAll(source.getVendorExtensions());
        }
    }
}
