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

public class SeeSummary {
    private String id;
    private String aggregatedId;
    private String title;
    private String href;

    public SeeSummary() {
        // no-op
    }

    public SeeSummary(final String aggregatedId, final String id, final String title, final String href) {
        setAggregatedId(aggregatedId);
        setId(id);
        setTitle(title);
        setHref(href);
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

    public String getTitle() {
        return title;
    }

    public String getHref() {
        return href;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public void setHref(final String href) {
        this.href = href;
    }
}
