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

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.models.Operation;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static java.util.Optional.ofNullable;

@Data
@NoArgsConstructor
public class EndpointWrapper {
    private String applicationId;
    private String endpointId;
    private String httpMethod;
    private String path;
    private Operation operation;
    private String humanReadablePath;

    @JsonProperty("_links")
    private Map<String, String> links = new HashMap<>();

    public EndpointWrapper(final String appId, final String endpointId, final String pathId,
                           final String httpMethod, final String path, final Operation operation) {
        this.applicationId = appId;
        this.endpointId = endpointId;
        this.humanReadablePath = pathId;
        this.httpMethod = ofNullable(httpMethod).map(s -> s.toLowerCase(Locale.ENGLISH)).orElse("-" /*can be null theorically*/);
        this.path = path;
        this.operation = operation;
    }
}
