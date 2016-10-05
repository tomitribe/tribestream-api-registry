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
 *
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
import org.tomitribe.tribestream.registryng.domain.HistoryItem;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.HistoryEntry;
import org.tomitribe.tribestream.registryng.entities.OpenAPIDocumentSerializer;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.resources.processor.ApplicationProcessor;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

import static java.util.stream.Collectors.toList;

@Path("/history/application")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
@NoArgsConstructor(force = true)
public class HistoryResource {
    private final Repository repository;
    private final OpenAPIDocumentSerializer documentSerializer;
    private final ApplicationProcessor processor;

    @GET
    @Path("/{applicationId}")
    public Response getApplicationHistory(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("per_page") @DefaultValue("20") final int perPage) {

        final int first = (page - 1) * perPage;

        final List<HistoryEntry<OpenApiDocument>> history = repository.getRevisions(OpenApiDocument.class, applicationId, first, perPage);

        List<HistoryItem> result = history.stream()
                .map(HistoryItem::new)
                .collect(toList());

        UriBuilder historyApplicationBaseUriBuilder = uriInfo.getBaseUriBuilder()
                .path("history/application/{applicationId}")
                .resolveTemplate("applicationId", applicationId);

        return Response.ok(result)
                .links(buildPaginationLinks(
                        historyApplicationBaseUriBuilder,
                        repository.getNumberOfRevisions(OpenApiDocument.class, applicationId),
                        page,
                        perPage))
                .links(buildRevisionLinks(
                        historyApplicationBaseUriBuilder,
                        result))
                .links(buildCurrentApplicationLink(uriInfo, applicationId))
                .build();
    }

    private Link buildCurrentApplicationLink(final UriInfo uriInfo, final String applicationId) {
        return Link.fromUriBuilder(uriInfo.getBaseUriBuilder()
                .path("application/{applicationId}")
                .resolveTemplate("applicationId", applicationId))
                .rel("application")
                .build();
    }

    private Link buildCurrentEndpointLink(final UriInfo uriInfo, final String applicationId, final String endpointId) {
        return Link.fromUriBuilder(uriInfo.getBaseUriBuilder()
                .path("application/{applicationId}/endpoint/{endpointId}")
                .resolveTemplate("applicationId", applicationId)
                .resolveTemplate("endpointId", endpointId))
                .rel("application")
                .build();
    }

    private Link[] buildRevisionLinks(final UriBuilder baseUriBuilder, List<HistoryItem> historyItems) {
        List<Link> result =
                historyItems.stream()
                        .map((HistoryItem historyItem) ->
                            Link.fromUriBuilder(baseUriBuilder.clone().path("/{revisionId}").resolveTemplate("revisionId", historyItem.getRevisionId()))
                                    .rel("revision " + historyItem.getRevisionId()).build()
                        )
                        .collect(toList());

        return result.toArray(new Link[result.size()]);
    }


    private Link[] buildPaginationLinks(final UriBuilder pageUriBuilder, final int numberOfRevisions, final int currentPage, final int currentPageSize) {

        List<Link> result = new ArrayList<>();

        result.add(createLink(pageUriBuilder, currentPage, currentPageSize, "self"));
        if (numberOfRevisions >= 1) {
            result.add(createLink(pageUriBuilder, 1, currentPageSize, "first"));
            result.add(createLink(pageUriBuilder, numberOfRevisions / currentPageSize + 1, currentPageSize, "last"));
        }
        if (numberOfRevisions > (currentPage * currentPageSize)) {
            result.add(createLink(pageUriBuilder, currentPage + 1, currentPageSize, "next"));
        }
        if (currentPage > 1) {
            result.add(createLink(pageUriBuilder, currentPage - 1, currentPageSize, "previous"));
        }
        return result.toArray(new Link[result.size()]);
    }

    @GET
    @Path("/{applicationId}/endpoint/{endpointId}")
    public Response getEndpointHistory(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("endpointId") final String endpointId,
            @QueryParam("page") @DefaultValue("1") final int page,
            @QueryParam("per_page") @DefaultValue("20") final int perPage) {

        final int first = (page - 1) * perPage;

        final List<HistoryEntry<Endpoint>> history = repository.getRevisions(Endpoint.class, endpointId, first, perPage);

        List<HistoryItem> result = history.stream()
                .map(HistoryItem::new)
                .collect(toList());

        UriBuilder endpointHistoryBaseUriBuilder = uriInfo.getBaseUriBuilder()
                .path("history/application/{applicationId}/endpoint/{endpointId}")
                .resolveTemplate("applicationId", applicationId)
                .resolveTemplate("endpointId", endpointId);

        return Response.ok(result)
                .links(buildPaginationLinks(
                        endpointHistoryBaseUriBuilder,
                        repository.getNumberOfRevisions(Endpoint.class, endpointId),
                        page,
                        perPage))
                .links(buildRevisionLinks(
                        endpointHistoryBaseUriBuilder,
                        result))
                .build();
    }

    @GET
    @Path("/{applicationId}/{revisionId:[0-9]+}")
    public Response getApplicationWithRevision(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("revisionId") final int revisionId) {

        OpenApiDocument application = repository.findByApplicationIdAndRevision(applicationId, revisionId);

        if (application == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        documentSerializer.postLoad(application);


        return Response.ok(processor.toWrapper(application))
                .links(buildCurrentApplicationLink(uriInfo, applicationId))
                //.links(buildEndpointLinks(uriInfo, application)) TODO: What links for historic instances?
                .build();
    }

    @GET
    @Path("/{applicationId}/endpoint/{endpointId}/{revisionId:[0-9]+}")
    public Response getEndpointWithRevision(
            @Context UriInfo uriInfo,
            @PathParam("applicationId") final String applicationId,
            @PathParam("endpointId") final String endpointId,
            @PathParam("revisionId") final int revisionId) {

        final Endpoint endpoint = repository.findEndpointByIdAndRevision(endpointId, revisionId);

        if (endpoint == null || endpoint.getApplication().getId() == null) {
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        documentSerializer.postLoad(endpoint);

        EndpointWrapper endpointWrapper = new EndpointWrapper(
                applicationId, endpointId, endpoint.getHumanReadablePath(),
                endpoint.getVerb(), endpoint.getPath(), endpoint.getOperation());

        return Response.ok(endpointWrapper)
                .links(buildCurrentEndpointLink(uriInfo, applicationId, endpointId))
                //.links(buildEndpointLinks(uriInfo, application)) TODO: What links for historic instances?
                .build();
    }


    private Link createLink(final UriBuilder uriBuilder, final int page, final int currentPageSize, final String rel) {
        return Link.fromUriBuilder(uriBuilder.clone().queryParam("page", page).queryParam("per_page", currentPageSize))
                .rel(rel)
                .build();
    }

}
