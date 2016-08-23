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
package com.tomitribe.tribestream.registryng.domain;

import com.tomitribe.tribestream.registryng.service.PathTransformUtil;
import java.util.Set;

public class SearchResult {
    private String deployableId;
    private String endpointId;
    private String aggregatedId;
    private String application;
    private String httpMethod;
    private String path;
    private String description;
    private Set<String> consumes;
    private Set<String> produces;
    private boolean secured;
    private boolean rateLimited;
    private float score;

    public SearchResult() {
        // no-op
    }

    public SearchResult(final String aggregatedId, final String deployableId, final String endpointId, final String application,
                        final String httpMethod, final String path,
                        final String description, final Set<String> consumes, final Set<String> produces,
                        final boolean secured, final boolean rateLimited, final float score) {
        this.aggregatedId = aggregatedId;
        this.deployableId = deployableId;
        this.endpointId = endpointId;
        this.application = application;
        this.httpMethod = httpMethod;
        this.path = PathTransformUtil.bracesToColon(path);
        this.description = description;
        this.consumes = consumes;
        this.produces = produces;
        this.secured = secured;
        this.score = score;
        this.rateLimited = rateLimited;
    }

    public String getAggregatedId() {
        return aggregatedId;
    }

    public void setAggregatedId(final String aggregatedId) {
        this.aggregatedId = aggregatedId;
    }

    public float getScore() {
        return score;
    }

    public void setScore(final float score) {
        this.score = score;
    }

    public String getDeployableId() {
        return deployableId;
    }

    public void setDeployableId(final String deployableId) {
        this.deployableId = deployableId;
    }

    public String getEndpointId() {
        return endpointId;
    }

    public void setEndpointId(final String endpointId) {
        this.endpointId = endpointId;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(final String application) {
        this.application = application;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }

    public void setRateLimited(boolean rateLimited) {
        this.rateLimited = rateLimited;
    }

    public boolean isRateLimited() {
        return rateLimited;
    }

    public Set<String> getConsumes() {
        return consumes;
    }

    public void setConsumes(Set<String> consumes) {
        this.consumes = consumes;
    }

    public Set<String> getProduces() {
        return produces;
    }

    public void setProduces(Set<String> produces) {
        this.produces = produces;
    }

}