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
package org.tomitribe.tribestream.registry.entities;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.Operation;
import io.swagger.models.Swagger;
import org.tomitribe.tribestream.registry.service.serialization.SwaggerJsonMapperProducer;

import javax.persistence.PostLoad;
import javax.persistence.PrePersist;
import java.io.StringWriter;

public class OpenAPIDocumentSerializer {
    private volatile ObjectMapper mapper;

    @PrePersist
    public void prepersist(Object entity) {
        if (entity instanceof OpenApiDocument) {
            OpenApiDocument document = (OpenApiDocument) entity;
            if (document.getSwagger() != null) {
                document.setDocument(convertToJson(document.getSwagger()));
            }
        } else if (entity instanceof Endpoint) {
            Endpoint endpoint = (Endpoint) entity;
            if (endpoint.getOperation() != null) {
                endpoint.setDocument(convertToJson(endpoint.getOperation()));
            }
        }
    }

    @PostLoad
    public void postLoad(Object entity) {
        if (entity instanceof OpenApiDocument) {
            OpenApiDocument document = (OpenApiDocument) entity;
            document.setSwagger(readJson(document.getDocument(), Swagger.class));
        } else if (entity instanceof Endpoint) {
            Endpoint endpoint = (Endpoint) entity;
            endpoint.setOperation(readJson(endpoint.getDocument(), Operation.class));
        }
    }

    private String convertToJson(Object object) {
        try (StringWriter sw = new StringWriter()) {
            mapper().writeValue(sw, object);
            sw.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private <T> T readJson(String s, Class<T> clazz) {
        try {
            return mapper().readValue(s, clazz);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ObjectMapper mapper() {
        if (mapper == null) {
            synchronized (this) {
                if (mapper == null) {
                    mapper = SwaggerJsonMapperProducer.lookup();
                }
            }
        }
        return mapper;
    }

}
