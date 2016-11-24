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
package org.tomitribe.tribestream.registryng.test.mock;

import javax.enterprise.context.ApplicationScoped;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;

import static java.util.Collections.list;
import static java.util.stream.Collectors.joining;

@Path("spy")
@ApplicationScoped
public class SpyResource {
    @GET
    public String copy(@Context final HttpServletRequest request) {
        return request.getMethod() + request.getRequestURI() + list(request.getHeaderNames()).stream().map(h -> h + "=" + request.getHeader(h)).collect(joining());
    }

    @POST
    public String copy(@Context final HttpServletRequest request, final String payload) {
        return copy(request) + payload;
    }
}
