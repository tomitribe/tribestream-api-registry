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

import org.tomitribe.tribestream.registryng.domain.ApplicationDetail;
import org.tomitribe.tribestream.registryng.domain.ApplicationSummary;
import org.tomitribe.tribestream.registryng.domain.CloudItem;
import org.tomitribe.tribestream.registryng.domain.EndpointDetail;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SeeDetail;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.search.SearchRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;

@Path("registry")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class RegistryResource {

    private final SearchEngine searchEngine;

    protected RegistryResource() {
        this.searchEngine = null;
    }

    @Inject
    public RegistryResource(final SearchEngine searchEngine) {
        this.searchEngine = searchEngine;
    }

    @GET
    public SearchPage getSearchPage(@Context final UriInfo uriInfo, @Context final HttpHeaders headers,
                                    @QueryParam("query") @DefaultValue("*") final String query,
                                    @QueryParam("tag") final List<String> tags,
                                    @QueryParam("category") final List<String> categories,
                                    @QueryParam("role") final List<String> roles,
                                    @QueryParam("app") final List<String> apps,
                                    @QueryParam("page") @DefaultValue("0") final int page,
                                    @QueryParam("count") @DefaultValue("1000") final int count) {
        final SearchRequest searchRequest = new SearchRequest(
            query,
            tags,
            categories,
            roles,
            apps,
            page,
            count
        );
        return searchEngine.search(searchRequest);
    }

    @GET
    @Path("all")
    public List<ApplicationSummary> applications(@Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        final List<ApplicationSummary> list = new ArrayList<ApplicationSummary>();
        /*
        for (final DeployableInfo deployable : tree.getChildren()) {
            final List<EndpointSummary> endpoints = new LinkedList<EndpointSummary>();
            final List<SeeSummary> sees = new LinkedList<SeeSummary>();

            for (final ApplicationInfo application : deployable.getChildren()) {
                for (final EndpointInfo endpoint : application.getChildren()) {
                    final EndpointInfo.URIInfo uri = endpoint.getUri().withBase(httpRules.base(uriInfo, headers));
                    final boolean secured = endpoint.getSecurity().getRolesAllowed().size() > 0;
                    endpoints.add(new EndpointSummary(
                        singleHashService.endpointAggregatedId(endpoint),
                        endpoint.getId(), uri.getHttpMethod(), strings.removeLeadingSlash(uri.getPath()), secured,
                        endpoint.getMetadata().getCategories(), endpoint.getMetadata().getTags(), endpoint.getDocs().getDoc().getDefaultDoc()));
                }

                for (final SeeInfo see : application.getSeeAlso()) {
                    sees.add(new SeeSummary(singleHashService.seeAggregatedId(see), see.getId(), see.getTitle(), see.getHref()));
                }
            }

            list.add(new ApplicationSummary(deployable.getId(), deployables.name(deployable), sees, endpoints));
        }
        */
        return list;
    }

    @GET
    @Path("tags")
    public List<CloudItem> tags() {
        return new ArrayList<>();
        /*
        return indexedWeights(new IndexesExtractor() {
            @Override
            public Iterable<String> indexes(final EndpointInfo endpoint) {
                return endpoint.getMetadata().getTags();
            }
        });
        */
    }

    @GET
    @Path("roles")
    public List<CloudItem> roles() {
        return new ArrayList<>();
        /*
        return indexedWeights(new IndexesExtractor() {
            @Override
            public Iterable<String> indexes(final EndpointInfo endpoint) {
                return endpoint.getSecurity().getRolesAllowed();
            }
        });
        */
    }

    @GET
    @Path("categories")
    public List<CloudItem> categories() {
        return new ArrayList<>();
        /*
        return indexedWeights(new IndexesExtractor() {
            @Override
            public Iterable<String> indexes(final EndpointInfo endpoint) {
                return endpoint.getMetadata().getCategories();
            }
        });
        */
    }

    @GET
    @Path("application/{deployableId}")
    public ApplicationDetail applicationDetail(@PathParam("deployableId")
                                               final String deployableId,

                                               @QueryParam("lang")
                                               @DefaultValue("en")
                                               final String lang,

                                               @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        //return applicationDetail(findDeployable(deployableId), lang, uriInfo, headers);
        return new ApplicationDetail();
    }

    @GET
    @Path("endpoint/{deployableId}/{endpointId}")
    public EndpointDetail endpointDetail(@PathParam("deployableId")
                                         final String deployableId,

                                         @PathParam("endpointId")
                                         final String endpointId,

                                         @QueryParam("lang")
                                         @DefaultValue("en")
                                         final String lang,

                                         @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        /*
        final DeployableInfo deployable = findDeployable(deployableId);
        for (final ApplicationInfo application : deployable.getChildren()) { // we shouldn't loop a lot and it avoids appId in the URL which makes it more user friendly
            final EndpointInfo endpoint = application.find(endpointId);
            if (endpoint != null) {
                return endpointDetail(endpoint, lang, uriInfo, headers);
            }
        }
        */
        throw new IllegalArgumentException("Can't find endpoint " + endpointId + " in application " + deployableId);
    }

    @GET
    @Path("see/{deployableId}/{seeId}")
    public SeeDetail seeDetail(@PathParam("deployableId")
                               final String deployableId,

                               @PathParam("seeId")
                               final String seeId,

                               @QueryParam("format")
                               @DefaultValue("html")
                               final String format) {
        /*
        final DeployableInfo deployable = findDeployable(deployableId);
        for (final ApplicationInfo application : deployable.getChildren()) { // we shouldn't loop a lot and it avoids appId in the URL which makes it more user friendly
            for (final SeeInfo see : application.getSeeAlso()) {
                if (see.getId().equals(seeId)) {
                    return seeDetail(deployable, deployableId, see, format);
                }
            }
        }*/

        throw new IllegalArgumentException("Can't find see " + seeId + " in application " + deployableId);
    }

}
