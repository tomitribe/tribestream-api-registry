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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.Operation;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

public class EndpointWrapper {

    private String httpMethod;

    private String path;

    private Operation operation;

    @JsonProperty("_links")
    private Map<String, String> links = new HashMap<>();


    public EndpointWrapper() {
    }

    public EndpointWrapper(final String httpMethod, final String path, final Operation operation) {
        this.httpMethod = httpMethod;
        this.path = path;
        this.operation = operation;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public void setLinks(Map<String, String> links) {
        this.links = links;
    }

    public Map<String, String> getLinks() {
        return links;
    }
    

    public String getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    public Operation getOperation() {
        return operation;
    }

    public void addLink(final String name, final URI uri) {
        links.put(name, uri.toASCIIString());
    }

}
