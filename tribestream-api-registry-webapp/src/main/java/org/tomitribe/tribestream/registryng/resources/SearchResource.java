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

import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.search.SearchRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("search")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SearchResource {

    protected final SearchEngine engine;

    @Inject
    public SearchResource(final SearchEngine engine) {
        this.engine = engine;
    }

    protected SearchResource() {
        this(null);
    }

    @HEAD
    @Path("reindex")
    public void reindex() {
        engine.resetIndex();
    }

    @GET
    @Path("pending")
    public int pendingTasks() {
        return engine.pendingTasks();
    }

    @GET
    public SearchPage search(@QueryParam("query") final String query,
                             @QueryParam("tag") final List<String> tags,
                             @QueryParam("category") final List<String> categories,
                             @QueryParam("role") final List<String> roles,
                             @QueryParam("app") final List<String> apps,
                             @QueryParam("page") @DefaultValue("0") final int page,
                             @QueryParam("max") @DefaultValue("20") final int max) {
        return engine.search(new SearchRequest(query, tags, categories, roles, apps, page, max));
    }

    // TODO: move it to registry resource? this has actually no link with searching
    // TODO: really do it since now it completely diverges
    @Path("top")
    @GET
    public SearchPage top(@Context final UriInfo uriInfo,
                          @Context final HttpHeaders headers,
                          @QueryParam("page") @DefaultValue("0") final int page,
                          @QueryParam("max") @DefaultValue("20") final int max) {
        throw new UnsupportedOperationException();
//        return new SearchPage();
/*
        // sort by deployment time
        final List<DeployableInfo> deployableInfos = new ArrayList<DeployableInfo>(tree.getChildren());
        Collections.sort(deployableInfos, new Comparator<DeployableInfo>() {

            @Override
            public int compare(DeployableInfo o1, DeployableInfo o2) {
                final long startTime1 = o1.getWebContext().getAppContext().getStartTime();
                final long startTime2 = o2.getWebContext().getAppContext().getStartTime();

                if (startTime1 == startTime2) {
                    return 0;
                }

                return startTime1 > startTime2 ? -1 : 1;
            }
        });

        // Collect all endpoints as search results
        final List<SearchResult> allSearchResults = new ArrayList<SearchResult>();
        for (final DeployableInfo deployable : deployableInfos) {

            for (final ApplicationInfo application : deployable.getChildren()) {
                for (final EndpointInfo endpoint : application.getChildren()) {

                    final boolean rateLimited = (endpoint.get(ConstraintInfo.class) != null) && (endpoint.get(ConstraintInfo.class).getApplicationLimit() != null)
                            && (endpoint.get(ConstraintInfo.class).getUserLimit("guest") != null);
                    allSearchResults.add(new SearchResult(
                                    singleHashService.endpointAggregatedId(endpoint),
                                    deployable.getId(),
                                    endpoint.getId(),
                                    deployables.name(deployable),
                                    endpoint.getUri().getHttpMethod(),
                                    endpoint.getUri().getPath(),
                                    endpoint.getDocs().getDoc().getDefaultDoc(),
                                    endpoint.getMime().getConsumes(), endpoint.getMime().getProduces(),
                                    endpoint.getSecurity().getRolesAllowed().size() > 0,
                                    rateLimited,
                                    0)
                    );
                }
            }
        }

        // paginate
        final List<SearchResult> results = new ArrayList<SearchResult>();
        int start = (page * max);

        if (start < allSearchResults.size()) {
            int end = start + max;
            if (end > allSearchResults.size()) {
                end = allSearchResults.size();
            }

            for (int i = start; i < end; i++) {
                results.add(allSearchResults.get(i));
            }
        }

        return new SearchPage(
                results, allSearchResults.size(), results.size(),
                new ArrayList<CloudItem>(), new ArrayList<CloudItem>(),
                new ArrayList<CloudItem>(), new ArrayList<CloudItem>());
                */
    }

//    protected SearchResource() {
//        this(null, null, null, null);
//    }
}
