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
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;
import org.tomitribe.tribestream.registryng.service.search.SearchRequest;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import java.util.List;

@Path("registry")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
@NoArgsConstructor(force = true)
@RequiredArgsConstructor(onConstructor = @__(@Inject))
public class RegistryResource {
    private final SearchEngine searchEngine;

    @GET
    public SearchPage getSearchPage(@Context final UriInfo uriInfo, @Context final HttpHeaders headers,
                                    @QueryParam("query") @DefaultValue("*") final String query,
                                    @QueryParam("tag") final List<String> tags,
                                    @QueryParam("category") final List<String> categories,
                                    @QueryParam("role") final List<String> roles,
                                    @QueryParam("app") final List<String> apps,
                                    @QueryParam("page") @DefaultValue("0") final int page,
                                    @QueryParam("count") @DefaultValue("1000") final int count) {
        final SearchPage searchPage = searchEngine.search(new SearchRequest(query, tags, categories, roles, apps, page, count));
        searchPage.getResults().forEach(searchResult -> searchResult.setLink(
                uriInfo.getBaseUriBuilder()
                        .path("/application/{applicationId}/endpoint/{endpointId}")
                        .resolveTemplate("applicationId", searchResult.getApplicationId())
                        .resolveTemplate("endpointId", searchResult.getEndpointId()).build().toASCIIString()));
        return searchPage;
    }

}
