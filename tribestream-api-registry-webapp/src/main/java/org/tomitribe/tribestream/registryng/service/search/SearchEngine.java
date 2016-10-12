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
package org.tomitribe.tribestream.registryng.service.search;

import org.tomitribe.tribestream.registryng.domain.CloudItem;
import org.tomitribe.tribestream.registryng.domain.SearchPage;
import org.tomitribe.tribestream.registryng.domain.SearchResult;
import org.tomitribe.tribestream.registryng.elasticsearch.ElasticsearchClient;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonString;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Logger;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.PROP_CATEGORIES;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.PROP_ROLES;
import static org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY;

// TODO: refactor this class to get rid of all that strings and
// use a json mapper instead
@ApplicationScoped
public class SearchEngine {
    private static final Logger LOGGER = Logger.getLogger(SearchEngine.class.getName());

    private static final String APPLICATION_ID_FIELD = "applicationId";
    private static final String APPLICATION_HUMAN_READABLE_NAME = "applicationHumanReadableName";
    private static final String APPLICATION_NAME = "applicationName";
    private static final String APPLICATION_VERSION = "applicationVersion";
    private static final String ENDPOINT_ID_FIELD = "endpointId";
    private static final String ENDPOINT_HUMAN_READABLE_NAME = "endpointHumanReadableName";
    private static final String VERB = "verb";
    private static final String HTTP_METHOD = "httpMethod";
    private static final String PATH = "path";
    private static final String DOC = "doc";
    private static final String SUMMARY = "summary";

    private final JsonBuilderFactory jsonFactory = Json.createBuilderFactory(emptyMap());

    @Inject
    private ElasticsearchClient elasticsearch;

    public SearchPage search(final SearchRequest request) {
        final int pageSize = request.getCount();

        final JsonObjectBuilder aggs = jsonFactory.createObjectBuilder()
                .add("tags", term("tag"))
                .add("categories", term("category"))
                .add("roles", term("role"))
                .add("applications", term("applicationName"));

        final JsonObjectBuilder query = jsonFactory.createObjectBuilder().add("aggs", aggs);
        if ((request.getQuery() != null && !"*".equals(request.getQuery()) && !request.getQuery().isEmpty())
                || (request.getCategories() != null && !request.getCategories().isEmpty())
                || (request.getTags() != null && !request.getTags().isEmpty())
                || (request.getRoles() != null && !request.getRoles().isEmpty())
                || (request.getApps() != null && !request.getApps().isEmpty())) {
            final JsonArrayBuilder must = jsonFactory.createArrayBuilder();

            ofNullable(request.getQuery())
                    .filter(q -> !q.isEmpty() && !"*".equals(q) /*default*/)
                    .ifPresent(q -> must.add(jsonFactory.createObjectBuilder()
                            .add("query_string", jsonFactory.createObjectBuilder()
                                    .add("query", q)))
                            .build());

            addDrillDown(request.getCategories(), must, "category");
            addDrillDown(request.getTags(), must, "tag");
            addDrillDown(request.getRoles(), must, "role");
            addDrillDown(request.getApps(), must, "context", "applicationName");

            query.add("query", jsonFactory.createObjectBuilder()
                    .add("bool", jsonFactory.createObjectBuilder()
                            .add("must", must)));
        }

        final JsonObject object = elasticsearch.search(query.build(), request.getPage() * pageSize, pageSize);

        final JsonObject hits = object.getJsonObject("hits");
        final JsonObject aggregations = object.getJsonObject("aggregations");
        final int total = hits.getInt("total");
        return new SearchPage(total == 0 ? new ArrayList<>() : hits.getJsonArray("hits").stream()
                .map(json -> JsonObject.class.cast(json).getJsonObject("_source"))
                .map(source -> new SearchResult(
                        getString(source, APPLICATION_ID_FIELD),
                        getString(source, ENDPOINT_ID_FIELD),
                        getString(source, APPLICATION_HUMAN_READABLE_NAME),
                        getString(source, ENDPOINT_HUMAN_READABLE_NAME),
                        getString(source, APPLICATION_NAME),
                        getString(source, APPLICATION_VERSION),
                        getString(source, VERB),
                        getString(source, PATH),
                        getString(source, SUMMARY),
                        getStrings(source, "category"),
                        getStrings(source, "tag"),
                        getStrings(source, "role"),
                        getDouble(source, "_score"),
                        null))
                .collect(toList()), total, request.getPage(),
                aggregationToSet(aggregations, "applications", request.getApps()),
                aggregationToSet(aggregations, "categories", request.getCategories()),
                aggregationToSet(aggregations, "tags", request.getTags()),
                aggregationToSet(aggregations, "roles", request.getRoles()));
    }

    public void indexEndpoint(final Endpoint endpoint) {
        LOGGER.info(() -> String.format("Indexing %s %s", endpoint.getVerb(), endpoint.getPath()));
        final String webCtx = endpoint.getApplication().getSwagger().getBasePath();
        final JsonObject document = createDocument(endpoint, webCtx);
        final String elasticsearchId = endpoint.getElasticsearchId();
        if (elasticsearchId != null) {
            elasticsearch.update(elasticsearchId, document);
        } else {
            endpoint.setElasticsearchId(elasticsearch.create(document).getString("_id"));
        }
    }

    public void deleteEndpoint(final Endpoint endpoint) {
        LOGGER.info(() -> String.format("Deleting Index %s %s", endpoint.getVerb(), endpoint.getPath()));
        ofNullable(endpoint.getElasticsearchId()).ifPresent(elasticsearch::delete);
    }

    private JsonObject createDocument(final Endpoint endpoint, final String webCtx) {
        final OpenApiDocument application = endpoint.getApplication();

        final JsonObjectBuilder eDoc = jsonFactory.createObjectBuilder();

        // id
        eDoc.add(APPLICATION_ID_FIELD, application.getId());
        eDoc.add(ENDPOINT_ID_FIELD, endpoint.getId());

        eDoc.add(APPLICATION_NAME, endpoint.getApplication().getSwagger().getInfo().getTitle());
        eDoc.add(APPLICATION_HUMAN_READABLE_NAME, endpoint.getApplication().getHumanReadableName());
        eDoc.add(APPLICATION_VERSION, endpoint.getApplication().getSwagger().getInfo().getVersion());

        // deployable
        if (webCtx != null && !webCtx.isEmpty()) {
            eDoc.add("context", webCtx);
        }
        if (application.getSwagger().getHost() != null) {
            eDoc.add("host", application.getSwagger().getHost());
        }

        final String defaultDoc = application.getSwagger().getInfo().getDescription();
        if (defaultDoc != null && !defaultDoc.isEmpty()) {
            eDoc.add("applicationDoc", defaultDoc);
        }

        // endpoint
        eDoc.add(ENDPOINT_HUMAN_READABLE_NAME, endpoint.getHumanReadablePath());
        eDoc.add(PATH, endpoint.getPath()); // shorter
        eDoc.add(HTTP_METHOD, endpoint.getVerb());
        eDoc.add(VERB, endpoint.getVerb());


        final List<String> categories = getExtensionProperty(endpoint, PROP_CATEGORIES, Collections::<String>emptyList);
        {
            final JsonArrayBuilder arrayBuilder = jsonFactory.createArrayBuilder();
            categories.forEach(arrayBuilder::add);
            eDoc.add("category", arrayBuilder);
        }
        if (endpoint.getOperation().getTags() != null) {
            final JsonArrayBuilder arrayBuilder = jsonFactory.createArrayBuilder();
            endpoint.getOperation().getTags().forEach(arrayBuilder::add);
            eDoc.add("tag", arrayBuilder);
        }
        final List<String> roles = getExtensionProperty(endpoint, PROP_ROLES, Collections::<String>emptyList);
        {
            final JsonArrayBuilder arrayBuilder = jsonFactory.createArrayBuilder();
            roles.forEach(arrayBuilder::add);
            eDoc.add("role", arrayBuilder);
        }
        final String summary = endpoint.getOperation().getSummary();
        if (summary != null && !summary.isEmpty()) {
            eDoc.add(SUMMARY, summary);
        }

        final String doc = endpoint.getOperation().getDescription();
        if (doc != null && !doc.isEmpty()) {
            eDoc.add(DOC, doc);
        }

        return eDoc.build();
    }

    private <T> List<T> getExtensionProperty(final Endpoint endpoint, final String extensionPropertyName, final Supplier<List<T>> defaultSupplier) {
        return (List<T>) Optional.ofNullable(endpoint.getOperation().getVendorExtensions())
                .map(vendorExtensions -> (Map<String, Object>) vendorExtensions.get(VENDOR_EXTENSION_KEY))
                .map(tapirExtension -> tapirExtension.get(extensionPropertyName))
                .orElseGet(defaultSupplier);
    }

    private Set<String> getStrings(final JsonObject object, final String key) {
        return object.containsKey(key) ? object.getJsonArray(key).getValuesAs(JsonString.class).stream()
                .map(JsonString::getString).collect(toSet()) : new HashSet<>();
    }

    private double getDouble(final JsonObject object, final String key) {
        return object.containsKey(key) ? object.getJsonNumber(key).doubleValue() : 0.;
    }

    private String getString(final JsonObject object, final String key) {
        return object.containsKey(key) ? object.getString(key) : null;
    }

    private void addDrillDown(final List<String> values, final JsonArrayBuilder builder, final String... names) {
        ofNullable(values).filter(c -> !c.isEmpty())
                .ifPresent(c -> c.forEach(it -> builder.add(jsonFactory.createObjectBuilder()
                        .add("term", names.length == 1 ?
                                jsonFactory.createObjectBuilder().add(names[0], it) :
                                jsonFactory.createObjectBuilder().add("bool", jsonFactory.createObjectBuilder().add("should",
                                        Stream.of(names)
                                                .map(n -> jsonFactory.createObjectBuilder().add("match", jsonFactory.createObjectBuilder().add(n, it)))
                                                .collect(jsonFactory::createArrayBuilder, JsonArrayBuilder::add, JsonArrayBuilder::add)

                                ))))));
    }

    private JsonObjectBuilder term(final String term) {
        return jsonFactory.createObjectBuilder()
                .add("terms", jsonFactory.createObjectBuilder()
                        .add("field", term));
    }

    private Set<CloudItem> aggregationToSet(final JsonObject aggregations, final String key, final Collection<String> filtered) {
        return aggregations.getJsonObject(key).getJsonArray("buckets").stream()
                .map(json -> {
                    final JsonObject o = JsonObject.class.cast(json);
                    return new CloudItem(o.getString("key"), o.getInt("doc_count"));
                })
                .filter(o -> filtered == null || !filtered.contains(o.getText())) // remove passed ones
                .collect(toSet());
    }
}
