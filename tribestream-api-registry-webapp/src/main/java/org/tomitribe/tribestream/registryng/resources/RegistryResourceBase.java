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
package org.tomitribe.tribestream.registryng.resources;

import org.tomitribe.tribestream.registryng.domain.ApplicationDetail;
import org.tomitribe.tribestream.registryng.domain.EndpointDetail;
import org.tomitribe.tribestream.registryng.domain.ErrorDetail;
import org.tomitribe.tribestream.registryng.domain.ParameterDetail;
import org.tomitribe.tribestream.registryng.domain.SeeSummary;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;
import org.tomitribe.tribestream.registryng.service.PathTransformUtil;
import io.swagger.models.ExternalDocs;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.parameters.AbstractSerializableParameter;
import io.swagger.models.parameters.Parameter;

import javax.inject.Inject;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

public abstract class RegistryResourceBase {

    @Inject
    private Repository repository;

    protected ApplicationDetail applicationDetail(final OpenApiDocument openApiDocument, final String lang, final UriInfo uriInfo, final HttpHeaders headers) {
        final Swagger swagger = openApiDocument.getSwagger();
        final String protocol = swagger.getSchemes().contains("https") ? "https" : "http";
        final String base = protocol + "://" + swagger.getHost() + swagger.getBasePath();

        final ExternalDocs seeAlso = swagger.getExternalDocs();
        final List<SeeSummary> sees = new ArrayList<SeeSummary>();
        if (seeAlso != null) {
            String id = repository.getApplicationId(swagger) + "-description";
            sees.add(new SeeSummary(id, id, seeAlso.getDescription(), seeAlso.getUrl()));
        }
        final List<ApplicationDetail.Detail> details = new ArrayList<>();
        details.add(
            new ApplicationDetail.Detail(
                Repository.getApplicationId(swagger),
                swagger.getInfo().getDescription(),
                sees,
                base));

        return new ApplicationDetail(repository.getApplicationId(swagger), details);

    }

    protected EndpointDetail endpointDetail(final Endpoint endpoint, final String lang, final UriInfo uriInfo, final HttpHeaders headers) {
        String basePath = endpoint.getApplication().getSwagger().getBasePath();
        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }
        //final String protocol = endpoint.getApplication().getSwagger().getSchemes().contains("https") ? "https" : "http";
        final String base = endpoint.getApplication().getSwagger().getHost() + basePath;

        final List<Parameter> parameters = endpoint.getOperation().getParameters();
        final List<ParameterDetail> params = new ArrayList<>();

        for (final Parameter parameter : parameters) {

            String type;
            boolean repeated;
            String defaultValue;

            if (parameter instanceof AbstractSerializableParameter) {
                AbstractSerializableParameter abstractSerializableParameter = (AbstractSerializableParameter) parameter;
                type = abstractSerializableParameter.getType();
                repeated = "array".equals(type) && (abstractSerializableParameter.getMaxItems() == null || abstractSerializableParameter.getMaxItems() > 1);
                defaultValue = abstractSerializableParameter.getDefaultValue();
            } else {
                type = parameter.toString();
                repeated = false;
                defaultValue = null;
            }

            params.add(new ParameterDetail(
                    parameter.getName(),
                    parameter.getIn(),
                    defaultValue,
                    type,
                    parameter.getDescription(),
                    repeated,
                    parameter.getRequired(),
                    Collections.<String>emptyList()
            ));
        }

        final Map<String, Response> errorMappings = endpoint.getOperation().getResponses();
        final List<ErrorDetail> errors = new ArrayList<>();
        for (final Map.Entry<String, Response> info : errorMappings.entrySet()) {
            if ("default".equals(info.getKey())) {
                continue;
            }
            errors.add(new ErrorDetail(
                    Integer.valueOf(info.getKey()),
                    (info.getValue().getExamples() == null || info.getValue().getExamples().isEmpty()) ? null : info.getValue().getExamples().values().iterator().next().toString(),
                    info.getValue().getDescription()));
        }

        final Collection<SeeSummary> sees = new ArrayList<SeeSummary>();
        if (endpoint.getOperation().getExternalDocs() != null) {
            sees.add(
                new SeeSummary(
                    endpoint.getId(),
                    endpoint.getId(),
                    endpoint.getOperation().getExternalDocs().getDescription(),
                    endpoint.getOperation().getExternalDocs().getUrl()));
        }

//        final ConstraintInfo constraints = endpoint.get(ConstraintInfo.class);
//        final List<ThrottlingInfo> userLimit = constraints != null ? new ArrayList<>(constraints.getAllUserLimits().values()) : null;

//        final ThrottlingInfo appLimit = constraints != null ? constraints.getApplicationLimit() : null;

//        Set<String> rolesAllowed = endpoint.getSecurity().getRolesAllowed();
//        final TomcatSecurityInfo tomcatSecurityInfo = endpoint.getTomcatSecurityInfo();
//        final boolean hasWebAppSecurityConstraints = tomcatSecurityInfo != null && tomcatSecurityInfo != TomcatSecurityInfo.NO_SECURITY;
//        if (hasWebAppSecurityConstraints) {
//            if (rolesAllowed == null) {
//                rolesAllowed = new HashSet<String>();
//            }
//            rolesAllowed.addAll(asList(tomcatSecurityInfo.getRoles()));
//        }


        return new EndpointDetail(
                new EndpointDetail.Id(
                        repository.getApplicationId(endpoint.getApplication().getSwagger()),
                        endpoint.getId(),
                        endpoint.getId()
                ),
                repository.getApplicationId(endpoint.getApplication().getSwagger()), // name
                new EndpointDetail.Docs(
                        endpoint.getOperation().getDescription(),
                        null, // getDoc(lang, endpoint.getDocs().getRequestDoc()),
                        null, // getDoc(lang, endpoint.getDocs().getResponseDoc()),
                        null  // getDoc(lang, endpoint.getDocs().getReturnDoc())
                ),
                new EndpointDetail.Uri(
                        endpoint.getVerb(), //uri.getHttpMethod(),
                        PathTransformUtil.bracesToColon(endpoint.getPath()), //strings.removeLeadingSlash(uri.getPath()),
                        base + PathTransformUtil.bracesToColon(endpoint.getPath()) // uri.getUri()
                ),
                new EndpointDetail.Samples(
                        null,
                        null,
                        null,
                        null,
                        getSampleResponse(endpoint, "json"),
                        getSampleResponse(endpoint, "xml"),
                        null,
                        null,
                        null,
                        null
                ),
                new EndpointDetail.MetaData(
                        Collections.<String>emptyList(), //metadata.getCategories(),
                        sees,
                        endpoint.getOperation().getTags() == null ? new HashSet<String>() : new HashSet<>(endpoint.getOperation().getTags()), //metadata.getTags(),
                    Arrays.asList(endpoint.getApplication().getSwagger().getInfo().getVersion()), //metadata.getApiVersions(),
                        null // metadata.getStatus() != null && metadata.getStatus().getType() != null ? metadata.getStatus().getType().name() : null
                ),
                new TreeSet<String>(), // rolesAllowed
                new EndpointDetail.Mime(
                        endpoint.getOperation().getConsumes() == null ? new HashSet<String>() : new HashSet<>(endpoint.getOperation().getConsumes()),
                        endpoint.getOperation().getProduces() == null ? new HashSet<String>() : new HashSet<>(endpoint.getOperation().getProduces())
                ),
                new EndpointDetail.Throttlings(),
                params,
                errors,
                endpoint.getVerb(),
                null,
//                hasWebAppSecurityConstraints ? new EndpointDetail.WebAppSecurity(
//                        tomcatSecurityInfo.getAuthMethod(), tomcatSecurityInfo.getTransportGuarantee(),
//                        new ArrayList<>(asList(tomcatSecurityInfo.getMandatoryHeaders()))) : null,
                null,
                PathTransformUtil.bracesToColon(endpoint.getPath()));
    }

    protected String getSampleResponse(final Endpoint endpoint, final String mimePart) {

        final String pattern = "2[0-9]{2}";
        // Take the smallest 2xx response or the default if there is none
        return getExample(endpoint, mimePart, pattern);
    }

    protected String getDefaultResponse(final Endpoint endpoint, final String mimePart) {
        final String pattern = "default";
        // Take the smallest 2xx response or the default if there is none
        return getExample(endpoint, mimePart, "default");
    }

    private String getExample(Endpoint endpoint, String mimePart, String statusCodePattern) {
        return endpoint.getOperation().getResponses().entrySet().stream()
                .filter((Map.Entry<String, Response> statusResponse) -> statusResponse.getKey().matches(statusCodePattern))
                .sorted(comparing(Map.Entry::getKey))
                .map(Map.Entry::getValue)
                .flatMap((Response r) -> r.getExamples() == null ? Stream.empty() : r.getExamples().entrySet().stream())
                .filter((Map.Entry<String, Object> mimeTypeExample) -> mimeTypeExample.getKey().contains(mimePart))
                .map(Map.Entry::getValue)
                .map(Object::toString)
                .findFirst()
                .orElse(null);
    }
/*
    private List<EndpointDetail.Throttling> toThrottlings(final List<ThrottlingInfo> throttlingInfos) {
        if (throttlingInfos == null || throttlingInfos.size() == 0) {
            return null;
        }

        final Map<String, EndpointDetail.Throttling> throttlingMap = new HashMap<>();

        for (final ThrottlingInfo throttlingInfo : throttlingInfos) {
            final String username = throttlingInfo.getName().replaceAll("^\\*$", "default");

            throttlingMap.put(username, new EndpointDetail.Throttling(
                username,
                throttlingInfo.getConcurrentLimit(),
                throttlingInfo.getRateWindow(),
                throttlingInfo.getRateLimit(),
                throttlingInfo.getRateUnit()));
        }

        final List<EndpointDetail.Throttling> list = new ArrayList<>();
        if (throttlingMap.containsKey("default")) {
            list.add(throttlingMap.remove("default"));
        }

        if (throttlingMap.containsKey("guest")) {
            list.add(throttlingMap.remove("guest"));
        }

        final List<EndpointDetail.Throttling> toSort = new ArrayList<>(throttlingMap.values());
        Collections.sort(toSort, new Comparator<EndpointDetail.Throttling>() {
            @Override
            public int compare(final EndpointDetail.Throttling o1, final EndpointDetail.Throttling o2) {
                return o1.getUser().compareTo(o2.getUser());
            }
        });

        list.addAll(toSort);
        return list;
    }
*/

        /*
    private ReturnTypeDetail getReturnType(final EndpointInfo endpoint, final String lang) {
        return null;
        if (endpoint == null || endpoint.getReturnType() == null || endpoint.getReturnType().getType() == null) {
            return null;
        }

        final Map<String, DocInfo> returnTypeDoc = endpoint.getDocs().getReturnTypeDoc();

        final List<ReturnTypeFieldDetail> fieldDetailList = new ArrayList<ReturnTypeFieldDetail>();
        final ReturnTypeDetail returnTypeDetail = new ReturnTypeDetail(endpoint.getReturnType().getType(), fieldDetailList);
        if (endpoint.getReturnType().getFieldInfo() != null) {
            for (FieldInfo fieldInfo : endpoint.getReturnType().getFieldInfo()) {
                String doc = "";

                if (returnTypeDoc.containsKey(fieldInfo.getName())) {
                    doc = returnTypeDoc.get(fieldInfo.getName()).getDoc(lang);
                    if (doc == null) {
                        doc = returnTypeDoc.get(fieldInfo.getName()).getDefaultDoc();
                    }
                }

                final ReturnTypeFieldDetail fieldDetail = new ReturnTypeFieldDetail(fieldInfo.getName(), fieldInfo.getType(), doc);
                fieldDetailList.add(fieldDetail);
            }
        }

        return returnTypeDetail;
    }
        */
/*
    protected SeeDetail seeDetail(final Identifiable<?> holder, final String parentId, final SeeInfo see, final String format) {
        final String key = see.getId() + "." + (format == null ? "" : format);
        SeeDetail existing = holder.get(key);
        if (existing == null) {
            final String aId = singleHashService.seeAggregatedId(see);
            if ("ignore".equals(format)) {
                existing = new SeeDetail(aId, parentId, see.getId(), see.getTitle(), see.getHref(), see.getIref(), see.getFormat(), see.getContent());
            } else {
                String outputFormat = format;
                String content;
                try {
                    content = docFormatter.generate(see.getFormat(), format, see.getContent());
                } catch (final IllegalArgumentException iae) {
                    content = see.getContent();
                    outputFormat = see.getFormat();
                }
                existing = new SeeDetail(aId, parentId, see.getId(), see.getTitle(), see.getHref(), see.getIref(), outputFormat, content);
            }
            holder.put(key, existing);
        }
        return existing;
    }

    private String getDoc(final String lang, final DocInfo doc) {
        final String docWithLang = doc.getDoc(lang);
        return docWithLang == null ? doc.getDefaultDoc() : docWithLang;
    }
    */
}
