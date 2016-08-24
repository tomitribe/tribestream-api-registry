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
import org.tomitribe.tribestream.registryng.domain.EndpointDetail;
import org.tomitribe.tribestream.registryng.domain.SeeDetail;
import org.tomitribe.tribestream.registryng.repository.Repository;

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

/**
 * basically same as {@see com.tomitribe.tribestream.registry.resource.RegistryResource} but
 * supporting single id entry ie endpoint will use <deployableId>T<endpointId>
 */
@Path("id/registry")
@ApplicationScoped
@Produces(MediaType.APPLICATION_JSON)
public class SingleHashRegistryResource extends RegistryResourceBase {

    @Inject
    private Repository repository;

    @GET
    @Path("application/{deployableId}")
    public ApplicationDetail applicationDetail(@PathParam("deployableId")
                                               final String deployableId,

                                               @QueryParam("lang")
                                               @DefaultValue("en")
                                               final String lang,

                                               @Context final UriInfo uriInfo, @Context final HttpHeaders headers) {
        return applicationDetail(repository.findByApplicationId(deployableId), lang, uriInfo, headers);
    }

    @GET
    @Path("endpoint/{endpointId}")
    public EndpointDetail endpointDetail(@PathParam("endpointId")
                                         final String endpointId,

                                         @QueryParam("lang")
                                         @DefaultValue("en")
                                         final String lang,

                                         @Context final UriInfo uriInfo,
                                         @Context final HttpHeaders headers) {
        return super.endpointDetail(repository.findEndpointById(endpointId), lang, uriInfo, headers);
    }

    @GET
    @Path("see/{seeId}")
    public SeeDetail seeDetail(@PathParam("seeId")
                               final String seeId,

                               @QueryParam("format")
                               @DefaultValue("html")
                               final String format) {
        throw new UnsupportedOperationException();
//        final DeployableInfo deployable = idService.findSeeDeployable(seeId);
//        final SeeInfo see = idService.findSee(seeId);
//        return seeDetail(deployable, deployable.getId(), see, format);
    }
}
