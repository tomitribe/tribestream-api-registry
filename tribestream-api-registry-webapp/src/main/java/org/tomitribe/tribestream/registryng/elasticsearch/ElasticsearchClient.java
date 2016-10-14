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
package org.tomitribe.tribestream.registryng.elasticsearch;

import lombok.extern.java.Log;
import org.apache.deltaspike.core.api.config.ConfigProperty;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.json.JsonObject;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import java.util.stream.Stream;

import static java.util.Optional.ofNullable;
import static java.util.logging.Level.SEVERE;
import static javax.ws.rs.client.Entity.entity;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON_TYPE;

@Log
@ApplicationScoped
public class ElasticsearchClient {
    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.base", defaultValue = "http://localhost:9200")
    private String base;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.index", defaultValue = "tribestream-api-registry")
    private String index;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.type.endpoint", defaultValue = "endpoint")
    private String endpointType;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.features")
    private String features;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.timeout.receive", defaultValue = "30000")
    private String receiveTimeout;

    @Inject
    @ConfigProperty(name = "tribe.registry.elasticsearch.timeout.connect", defaultValue = "30000")
    private String connectTimeout;

    private Client client;

    @PostConstruct
    private void init() {
        client = ClientBuilder.newClient()
                // CXF
                .property("http.connection.timeout", connectTimeout)
                .property("http.receive.timeout", receiveTimeout)
                // Jersey
                .property("jersey.config.client.connectTimeout", connectTimeout)
                .property("jersey.config.client.readTimeout", receiveTimeout);

        ofNullable(features).ifPresent(f -> Stream.of(f.split(",")).forEach(v -> {
            try {
                client.register(Thread.currentThread().getContextClassLoader().loadClass(v).newInstance());
            } catch (final InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                log.warning("Can't activate debugging on Elasticsearch client: " + e.getMessage());
            }
        }));
    }

    public JsonObject create(final JsonObject data) {
        return client.target(base).path("{index}/{type}")
                .resolveTemplate("index", index)
                .resolveTemplate("type", endpointType)
                .request(APPLICATION_JSON_TYPE)
                .post(entity(data, APPLICATION_JSON_TYPE), JsonObject.class);

    }

    public JsonObject update(final String id, final JsonObject data) {
        return client.target(base).path("{index}/{type}/{id}")
                .resolveTemplate("index", index)
                .resolveTemplate("type", endpointType)
                .resolveTemplate("id", id)
                .request(APPLICATION_JSON_TYPE)
                .post(entity(data, APPLICATION_JSON_TYPE), JsonObject.class);
    }

    public void delete(final String id) {
        try {
            client.target(base).path("{index}/{type}/{id}")
                    .resolveTemplate("index", index)
                    .resolveTemplate("type", endpointType)
                    .resolveTemplate("id", id)
                    .request(APPLICATION_JSON_TYPE)
                    .delete(JsonObject.class);
        } catch (final NotFoundException nfe) {
            // that's as if it was deleted
        }
    }

    public JsonObject find(final String id) {
        return client.target(base).path("{index}/{type}/{id}")
                .resolveTemplate("index", index)
                .resolveTemplate("type", endpointType)
                .resolveTemplate("id", id)
                .request(APPLICATION_JSON_TYPE)
                .get(JsonObject.class);
    }

    public JsonObject search(final JsonObject query, final long from, final long size) {
        try {
            WebTarget target = client.target(base).path("_search");
            if (from >= 0) {
                target = target.queryParam("from", from);
            }
            if (size > 0) {
                target = target.queryParam("size", size);
            }
            final Invocation.Builder builder = target.request(APPLICATION_JSON_TYPE);
            return query == null ? builder.get(JsonObject.class) : builder.post(entity(query, APPLICATION_JSON_TYPE), JsonObject.class);
        } catch (final WebApplicationException wae) {
            log.log(SEVERE, wae.getMessage() + ": " + ofNullable(wae.getResponse()).map(r -> r.readEntity(String.class)).orElse("-"), wae);
            throw wae;
        }
    }

    @PreDestroy
    private void destroy() {
        client.close();
    }
}
