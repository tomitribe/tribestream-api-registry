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

public class SearchPage {
    private Collection<SearchResult> results;
    private Collection<CloudItem> applications;
    private Collection<CloudItem> categories;
    private Collection<CloudItem> tags;
    private Collection<CloudItem> roles;
    private int total;
    private int current;

    public SearchPage() {
        // no-op
    }

    public SearchPage(final Collection<SearchResult> results, final int total, final int current,
                      final Collection<CloudItem> aggregatedApplications,
                      final Collection<CloudItem> aggregatedCategories,
                      final Collection<CloudItem> aggregatedTags,
                      final Collection<CloudItem> aggregatedRoles) {
        this.results = results;
        this.total = total;
        this.current = current;
        this.applications = aggregatedApplications;
        this.categories = aggregatedCategories;
        this.tags = aggregatedTags;
        this.roles = aggregatedRoles;
    }

    public Collection<CloudItem> getApplications() {
        return applications;
    }

    public void setApplications(final Collection<CloudItem> applications) {
        this.applications = applications;
    }

    public Collection<CloudItem> getCategories() {
        return categories;
    }

    public void setCategories(final Collection<CloudItem> categories) {
        this.categories = categories;
    }

    public Collection<CloudItem> getTags() {
        return tags;
    }

    public Collection<CloudItem> getRoles() {
        return roles;
    }

    public void setRoles(Collection<CloudItem> roles) {
        this.roles = roles;
    }

    public void setTags(final Collection<CloudItem> tags) {
        this.tags = tags;
    }

    public Collection<SearchResult> getResults() {
        return results;
    }

    public void setResults(final Collection<SearchResult> results) {
        this.results = results;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(final int total) {
        this.total = total;
    }

    public int getCurrent() {
        return current;
    }

    public void setCurrent(final int current) {
        this.current = current;
    }
}
