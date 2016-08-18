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

import java.util.List;

public class ApplicationSummary {
    private String id;
    private String name;
    private List<SeeSummary> sees;
    private List<EndpointSummary> endpoints;

    public ApplicationSummary() {
        // no-op
    }

    public ApplicationSummary(final String id, final String name, final List<SeeSummary> sees, final List<EndpointSummary> endpoints) {
        setId(id);
        setName(name);
        setSees(sees);
        setEndpoints(endpoints);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public List<SeeSummary> getSees() {
        return sees;
    }

    public List<EndpointSummary> getEndpoints() {
        return endpoints;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setSees(final List<SeeSummary> sees) {
        this.sees = sees;
    }

    public void setEndpoints(final List<EndpointSummary> endpoints) {
        this.endpoints = endpoints;
    }
}
