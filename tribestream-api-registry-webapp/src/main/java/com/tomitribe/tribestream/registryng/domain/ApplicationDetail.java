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

public class ApplicationDetail {
    private String id;

    // JAX-RS Application, don't dedicate them a page for now since size() == 1 in most of cases
    // but keep a list in case we need it to avoid to break the API
    private List<Detail> details;

    public ApplicationDetail() {
        // no-op
    }

    public ApplicationDetail(final String id, final List<Detail> details) {
        setId(id);
        setDetails(details);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public List<Detail> getDetails() {
        return details;
    }

    public void setDetails(final List<Detail> details) {
        this.details = details;
    }

    public static class Detail {
        private String name;
        private String doc;
        private String baseUrl;
        private List<SeeSummary> sees;

        public Detail() {
            // no-op
        }

        public Detail(final String name, final String doc, final List<SeeSummary> sees, final String baseUrl) {
            setName(name);
            setDoc(doc);
            setSees(sees);
            setBaseUrl(baseUrl);
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getDoc() {
            return doc;
        }

        public void setDoc(final String doc) {
            this.doc = doc;
        }

        public List<SeeSummary> getSees() {
            return sees;
        }

        public void setSees(final List<SeeSummary> sees) {
            this.sees = sees;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
