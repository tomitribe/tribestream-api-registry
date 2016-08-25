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

public class SeeDetail {
    private String deployableId;
    private String id;
    private String aggregatedId;
    private String title;
    private String href;
    private String iref;
    private String format;
    private String content;

    public SeeDetail() {
        // no-op
    }

    public SeeDetail(final String aggregatedId, final String deployableId, final String id, final String title,
                     final String href, final String iref, final String format, final String content) {
        setAggregatedId(aggregatedId);
        setDeployableId(deployableId);
        setId(id);
        setTitle(title);
        setHref(href);
        setIref(iref);
        setFormat(format);
        setContent(content);
    }

    public String getAggregatedId() {
        return aggregatedId;
    }

    public void setAggregatedId(final String aggregatedId) {
        this.aggregatedId = aggregatedId;
    }

    public String getDeployableId() {
        return deployableId;
    }

    public void setDeployableId(final String deployableId) {
        this.deployableId = deployableId;
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    public String getHref() {
        return href;
    }

    public void setHref(final String href) {
        this.href = href;
    }

    public String getIref() {
        return iref;
    }

    public void setIref(final String iref) {
        this.iref = iref;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(final String format) {
        this.format = format;
    }

    public String getContent() {
        return content;
    }

    public void setContent(final String content) {
        this.content = content;
    }
}
