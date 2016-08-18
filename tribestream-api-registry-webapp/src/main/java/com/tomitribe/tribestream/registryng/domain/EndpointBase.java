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

public class EndpointBase {
    private String id;
    private String aggregatedId;
    private String httpMethod;
    private String path;
    private boolean secured;

    public EndpointBase() {
        // no-op
    }

    public EndpointBase(final String aggregatedId, final String id, final String httpMethod, final String path, final boolean secured) {
        this.id = id;
        this.aggregatedId = aggregatedId;
        this.httpMethod = httpMethod;
        this.path = path;
        this.secured = secured;
    }

    public String getAggregatedId() {
        return aggregatedId;
    }

    public void setAggregatedId(final String aggregatedId) {
        this.aggregatedId = aggregatedId;
    }

    public String getId() {
        return id;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setHttpMethod(final String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setPath(final String path) {
        this.path = path;
    }

    public boolean isSecured() {
        return secured;
    }

    public void setSecured(boolean secured) {
        this.secured = secured;
    }
}
