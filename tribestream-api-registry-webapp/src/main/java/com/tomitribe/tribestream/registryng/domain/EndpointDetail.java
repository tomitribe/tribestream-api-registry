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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class EndpointDetail {
    private Id id;
    private String name;
    private String methodName;
    private Docs doc;
    private Uri uri;
    private Samples samples;
    private MetaData metadata;
    private Collection<String> roles;
    private List<ErrorDetail> errors;
    private List<ParameterDetail> params;
    private Mime mime;
    private Throttlings throttlings;
    private ReturnTypeDetail returnTypeDetail;
    private WebAppSecurity webAppSecurity;
    private String mapping;

    public EndpointDetail() {
        // no-op
    }

    public EndpointDetail(final Id id,
                          final String name,
                          final Docs doc,
                          final Uri uri,
                          final Samples samples,
                          final MetaData metadata,
                          final Collection<String> roles,
                          final Mime mime,
                          final Throttlings throttlings,
                          final List<ParameterDetail> params,
                          final List<ErrorDetail> errors,
                          final String methodName,
                          final ReturnTypeDetail returnTypeDetail,
                          final WebAppSecurity webAppSecurity,
                          final String mapping) {
        setId(id);
        setName(name);
        setUri(uri);
        setParams(params);
        setSamples(samples);
        setDoc(doc);
        setMetadata(metadata);
        setRoles(roles);
        setMime(mime);
        setThrottlings(throttlings);
        setErrors(errors);
        setMethodName(methodName);
        setReturnTypeDetail(returnTypeDetail);
        setWebAppSecurity(webAppSecurity);
        setMapping(mapping);
    }

    public String getMapping() {
        return mapping;
    }

    public void setMapping(final String mapping) {
        this.mapping = mapping;
    }

    public WebAppSecurity getWebAppSecurity() {
        return webAppSecurity;
    }

    public void setWebAppSecurity(final WebAppSecurity webAppSecurity) {
        this.webAppSecurity = webAppSecurity;
    }

    public List<ErrorDetail> getErrors() {
        return errors;
    }

    public void setErrors(final List<ErrorDetail> errors) {
        this.errors = errors;
    }

    public Throttlings getThrottlings() {
        return throttlings;
    }

    public void setThrottlings(final Throttlings throttlings) {
        this.throttlings = throttlings;
    }

    public Mime getMime() {
        return mime;
    }

    public void setMime(final Mime mime) {
        this.mime = mime;
    }

    public Collection<String> getRoles() {
        return roles;
    }

    public void setRoles(final Collection<String> rolesAllowed) {
        this.roles = rolesAllowed;
    }

    public MetaData getMetadata() {
        return metadata;
    }

    public void setMetadata(final MetaData metadata) {
        this.metadata = metadata;
    }

    public Docs getDoc() {
        return doc;
    }

    public void setDoc(final Docs doc) {
        this.doc = doc;
    }

    public void setId(final Id id) {
        this.id = id;
    }

    public Id getId() {
        return id;
    }

    public Uri getUri() {
        return uri;
    }

    public String getResourceUrl() {
        return uri.getUri();
    }

    public void setUri(final Uri uri) {
        this.uri = uri;
    }

    public Samples getSamples() {
        return samples;
    }

    public void setSamples(final Samples samples) {
        this.samples = samples;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public List<ParameterDetail> getParams() {
        return params;
    }

    public void setParams(final List<ParameterDetail> params) {
        this.params = params;
    }

    public String getMethodName() {
        return methodName;
    }

    public void setMethodName(final String methodName) {
        this.methodName = methodName;
    }

    public ReturnTypeDetail getReturnTypeDetail() {
        return returnTypeDetail;
    }

    public void setReturnTypeDetail(ReturnTypeDetail returnTypeDetail) {
        this.returnTypeDetail = returnTypeDetail;
    }

    public static class Id {
        private String deployableId;
        private String id;
        private String aggregatedId;

        public Id() {
            // no-op
        }

        public Id(final String deployableId, final String id, final String aggregatedId) {
            this.aggregatedId = aggregatedId;
            this.deployableId = deployableId;
            this.id = id;
        }

        public String getAggregatedId() {
            return aggregatedId;
        }

        public void setAggregatedId(String aggregatedId) {
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
    }

    public static class Uri {
        private String httpMethod;
        private String path;
        private String uri;

        public Uri() {
            // no-op
        }

        public Uri(final String httpMethod, final String path, final String uri) {
            this.httpMethod = httpMethod;
            this.path = path;
            this.uri = uri;
        }

        public String getHttpMethod() {
            return httpMethod;
        }

        public void setHttpMethod(final String httpMethod) {
            this.httpMethod = httpMethod;
        }

        public String getPath() {
            return path;
        }

        public void setPath(final String path) {
            this.path = path;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(final String uri) {
            this.uri = uri;
        }
    }

    public static class Samples {
        private String sampleHttpRequest;
        private String sampleCurlRequest;
        private String sampleJsonRequestBody;
        private String sampleXmlRequestBody;
        // The samples computed from the return type of the method
        private String sampleJsonResponse;
        private String sampleXmlResponse;
        // The samples computed from the @ResponseMappings
        private Map<Integer, String> sampleJsonResponses;
        private Map<Integer, String> sampleXmlResponses;
        private String defaultSampleJsonResponse;
        private String defaultSampleXmlResponse;

        public Samples() {
            // no-op
        }

        public Samples(final String sampleHttpRequest, final String sampleCurlRequest,
                       final String sampleJsonRequestBody, final String sampleXmlRequestBody,
                       final String sampleJsonResponse, final String sampleXmlResponse,
                       final Map<Integer, String> sampleJsonResponses, final Map<Integer, String> sampleXmlResponses,
                       final String defaultSampleJsonResponse, final String defaultSampleXmlResponse) {
            this.sampleHttpRequest = sampleHttpRequest;
            this.sampleCurlRequest = sampleCurlRequest;
            this.sampleJsonRequestBody = sampleJsonRequestBody;
            this.sampleXmlRequestBody = sampleXmlRequestBody;
            this.sampleJsonResponse = sampleJsonResponse;
            this.sampleXmlResponse = sampleXmlResponse;
            this.sampleJsonResponses = sampleJsonResponses;
            this.sampleXmlResponses = sampleXmlResponses;
            this.defaultSampleJsonResponse = defaultSampleJsonResponse;
            this.defaultSampleXmlResponse = defaultSampleXmlResponse;
        }

        public String getSampleHttpRequest() {
            return sampleHttpRequest;
        }

        public void setSampleHttpRequest(final String sampleHttpRequest) {
            this.sampleHttpRequest = sampleHttpRequest;
        }

        public String getSampleCurlRequest() {
            return sampleCurlRequest;
        }

        public void setSampleCurlRequest(final String sampleCurlRequest) {
            this.sampleCurlRequest = sampleCurlRequest;
        }

        public String getSampleJsonRequestBody() {
            return sampleJsonRequestBody;
        }

        public void setSampleJsonRequestBody(final String sampleJsonRequestBody) {
            this.sampleJsonRequestBody = sampleJsonRequestBody;
        }

        public String getSampleXmlRequestBody() {
            return sampleXmlRequestBody;
        }

        public void setSampleXmlRequestBody(final String sampleXmlRequestBody) {
            this.sampleXmlRequestBody = sampleXmlRequestBody;
        }

        public String getSampleJsonResponse() {
            return sampleJsonResponse;
        }

        public void setSampleJsonResponse(String sampleJsonResponse) {
            this.sampleJsonResponse = sampleJsonResponse;
        }

        public String getSampleXmlResponse() {
            return sampleXmlResponse;
        }

        public void setSampleXmlResponse(String sampleXmlResponse) {
            this.sampleXmlResponse = sampleXmlResponse;
        }

        public Map<Integer, String> getSampleJsonResponses() {
            return sampleJsonResponses;
        }

        public void setSampleJsonResponses(final Map<Integer, String> sampleJsonResponses) {
            this.sampleJsonResponses = sampleJsonResponses;
        }

        public Map<Integer, String> getSampleXmlResponses() {
            return sampleXmlResponses;
        }

        public void setSampleXmlResponses(final Map<Integer, String> sampleXmlResponses) {
            this.sampleXmlResponses = sampleXmlResponses;
        }

        public String getDefaultSampleJsonResponse() {
            return defaultSampleJsonResponse;
        }

        public void setDefaultSampleJsonResponse(String defaultSampleJsonResponse) {
            this.defaultSampleJsonResponse = defaultSampleJsonResponse;
        }

        public String getDefaultSampleXmlResponse() {
            return defaultSampleXmlResponse;
        }

        public void setDefaultSampleXmlResponse(String defaultSampleXmlResponse) {
            this.defaultSampleXmlResponse = defaultSampleXmlResponse;
        }
    }

    public static class Docs {
        private  String doc;
        private String returnDoc;
        private String requestDoc;
        private String responseDoc;

        public Docs() {
            // no-op
        }

        public Docs(final String doc, final String requestDoc, final String responseDoc, final String returnDoc) {
            this.doc = doc;
            this.returnDoc = returnDoc;
            this.requestDoc = requestDoc;
            this.responseDoc = responseDoc;
        }

        public String getDoc() {
            return doc;
        }

        public String getReturnDoc() {
            return returnDoc;
        }

        public String getRequestDoc() {
            return requestDoc;
        }

        public String getResponseDoc() {
            return responseDoc;
        }

        public void setDoc(final String doc) {
            this.doc = doc;
        }

        public void setReturnDoc(final String returnDoc) {
            this.returnDoc = returnDoc;
        }

        public void setRequestDoc(final String requestDoc) {
            this.requestDoc = requestDoc;
        }

        public void setResponseDoc(final String responseDoc) {
            this.responseDoc = responseDoc;
        }
    }



    public static class MetaData {
        private Collection<String> categories;
        private Collection<SeeSummary> sees;
        private Collection<String> tags;
        private Collection<String> apiVersions;
        private String status;

        public MetaData() {
            // no-op
        }

        public MetaData(final Collection<String> categories, final Collection<SeeSummary> seeAlso,
                            final Set<String> tags, final Collection<String> apiVersions,
                            final String status) {

            this.categories = categories;
            this.sees = seeAlso;
            this.tags = tags;
            this.apiVersions = apiVersions;
            this.status = status;
        }

        public Collection<String> getCategories() {
            return categories;
        }

        public Collection<SeeSummary> getSees() {
            return sees;
        }

        public Collection<String> getTags() {
            return tags;
        }

        public Collection<String> getApiVersions() {
            return apiVersions;
        }

        public String getStatus() {
            return status;
        }

        public void setCategories(final Collection<String> categories) {
            this.categories = categories;
        }

        public void setSees(final Collection<SeeSummary> seeAlso) {
            this.sees = seeAlso;
        }

        public void setTags(final Collection<String> tags) {
            this.tags = tags;
        }

        public void setApiVersions(final Collection<String> apiVersions) {
            this.apiVersions = apiVersions;
        }

        public void setStatus(final String status) {
            this.status = status;
        }

    }

    public static class Mime {
        private Collection<String> consumes;
        private Collection<String> produces;

        public Mime() {
            // no-op
        }

        public Mime(final Collection<String> consumes, final Collection<String> produces) {
            this.consumes = consumes;
            this.produces = produces;
        }

        public Collection<String> getProduces() {
            return produces;
        }

        public Collection<String> getConsumes() {
            return consumes;
        }

        public void setConsumes(final Collection<String> consumes) {
            this.consumes = consumes;
        }

        public void setProduces(final Collection<String> produces) {
            this.produces = produces;
        }
    }

    public static class Throttling {
        private Integer concurrentLimit;
        private Integer rateWindow;
        private Integer rateLimit;
        private String rateUnit;
        private String user;

        public Throttling() {
            // no-op
        }

        public Throttling(final String user, final Integer concurrentLimit, final Integer rateWindow, final Integer rateLimit, final String rateUnit) {
            this.user = user;
            this.concurrentLimit = concurrentLimit;
            this.rateWindow = rateWindow;
            this.rateLimit = rateLimit;
            this.rateUnit = rateUnit;
        }

        public Integer getConcurrentLimit() {
            return concurrentLimit;
        }

        public Integer getRateWindow() {
            return rateWindow;
        }

        public Integer getRateLimit() {
            return rateLimit;
        }

        public String getRateUnit() {
            return rateUnit;
        }

        public void setConcurrentLimit(final Integer concurrentLimit) {
            this.concurrentLimit = concurrentLimit;
        }

        public void setRateWindow(final Integer rateWindow) {
            this.rateWindow = rateWindow;
        }

        public void setRateLimit(final Integer rateLimit) {
            this.rateLimit = rateLimit;
        }

        public void setRateUnit(final String rateUnit) {
            this.rateUnit = rateUnit;
        }

        public String getUser() {
            return user;
        }

        public void setUser(final String user) {
            this.user = user;
        }
    }

    public static class Throttlings {
        private List<Throttling> user;
        private Throttling application;

        public Throttlings() {
            // no-op
        }

        public Throttlings(final List<Throttling> user, final Throttling application) {
            this.user = user;
            this.application = application;
        }

        public List<Throttling> getUser() {
            return user;
        }

        public void setUser(final List<Throttling> user) {
            this.user = user;
        }

        public Throttling getApplication() {
            return application;
        }

        public void setApplication(final Throttling application) {
            this.application = application;
        }
    }

    public static class WebAppSecurity {
        private String authMethod;
        private String transportGuarantee;
        private Collection<String> mandatoryHeaders;

        public WebAppSecurity() {
            // no-op
        }

        public WebAppSecurity(final String authMethod, final String transportGuarantee, final Collection<String> mandatoryHeaders) {
            this.authMethod = authMethod;
            this.transportGuarantee = transportGuarantee;
            this.mandatoryHeaders = mandatoryHeaders;
        }

        public Collection<String> getMandatoryHeaders() {
            return mandatoryHeaders;
        }

        public void setMandatoryHeaders(final Collection<String> mandatoryHeaders) {
            this.mandatoryHeaders = mandatoryHeaders;
        }

        public String getAuthMethod() {
            return authMethod;
        }

        public void setAuthMethod(final String authMethod) {
            this.authMethod = authMethod;
        }

        public String getTransportGuarantee() {
            return transportGuarantee;
        }

        public void setTransportGuarantee(final String transportGuarantee) {
            this.transportGuarantee = transportGuarantee;
        }
    }
}
