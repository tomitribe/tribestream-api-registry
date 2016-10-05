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

import lombok.Data;
import lombok.NoArgsConstructor;
import org.tomitribe.tribestream.registryng.service.PathTransformUtil;

import java.util.Set;

@Data
@NoArgsConstructor
public class SearchResult {
    private String applicationId;
    private String endpointId;
    private String applicationName;
    private String endpointName;
    private String aggregatedId;
    private String application;
    private String applicationVersion;
    private String httpMethod;
    private String path;
    private String description;
    private Set<String> consumes;
    private Set<String> produces;
    private boolean secured;
    private boolean rateLimited;
    private float score;
    private String link;

    public SearchResult(final String aggregatedId, final String applicationId, final String endpointId,
                        final String humanAppName, final String humanEndpointName,
                        final String application, final String applicationVersion,
                        final String httpMethod, final String path,
                        final String description, final Set<String> consumes, final Set<String> produces,
                        final boolean secured, final boolean rateLimited, final float score) {
        this.aggregatedId = aggregatedId;
        this.applicationId = applicationId;
        this.endpointId = endpointId;
        this.application = application;
        this.applicationName = humanAppName;
        this.endpointName = humanEndpointName;
        this.applicationVersion = applicationVersion;
        this.httpMethod = httpMethod;
        this.path = PathTransformUtil.bracesToColon(path);
        this.description = description;
        this.consumes = consumes;
        this.produces = produces;
        this.secured = secured;
        this.score = score;
        this.rateLimited = rateLimited;
    }
}