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
package org.tomitribe.tribestream.registryng.domain;

import org.tomitribe.tribestream.registryng.service.PathTransformUtil;

import java.util.Set;

public class SearchResult {
    private String applicationId;
    private String endpointId;
    private String application;
    private String httpMethod;
    private String path;
    private String description;
    private Set<String> consumes;
    private Set<String> produces;
    private String link;

    public SearchResult() {
        // no-op
    }

    public SearchResult(final String aggregatedId, final String applicationId, final String endpointId,
                        final String application, final String applicationVersion,
                        final String httpMethod, final String path,
                        final String description, final Set<String> consumes, final Set<String> produces,
                        final boolean secured, final boolean rateLimited, final float score) {
        this.applicationId = applicationId;
        this.endpointId = endpointId;
        this.application = application;
        this.httpMethod = httpMethod;
        this.path = PathTransformUtil.bracesToColon(path);
        this.description = description;
        this.consumes = consumes;
        this.produces = produces;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public String getApplication() {
        return application;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public String getDescription() {
        return description;
    }

    @SuppressWarnings("unused")
    public Set<String> getConsumes() {
        return consumes;
    }

    @SuppressWarnings("unused")
    public Set<String> getProduces() {
        return produces;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getLink() {
        return link;
    }
}