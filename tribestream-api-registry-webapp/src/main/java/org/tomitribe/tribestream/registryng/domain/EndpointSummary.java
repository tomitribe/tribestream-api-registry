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

import java.util.Collection;

public class EndpointSummary extends EndpointBase {
    private Collection<String> categories;
    private Collection<String> tags;
    private String description;

    public EndpointSummary() {
        // no-op
    }

    public EndpointSummary(final String aggregatedId, final String id, final String httpMethod, final String path, final boolean secured,
                           final Collection<String> categories, final Collection<String> tags, String description) {
        super(aggregatedId, id, httpMethod, path, secured);
        this.categories = categories;
        this.tags = tags;
        this.description = description;
    }

    public Collection<String> getCategories() {
        return categories;
    }

    public void setCategories(final Collection<String> categories) {
        this.categories = categories;
    }

    public Collection<String> getTags() {
        return tags;
    }

    public void setTags(final Collection<String> tags) {
        this.tags = tags;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(final String description) {
        this.description = description;
    }
}
