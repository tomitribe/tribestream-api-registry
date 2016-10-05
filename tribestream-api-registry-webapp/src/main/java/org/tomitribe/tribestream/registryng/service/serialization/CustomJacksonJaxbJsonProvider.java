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
package org.tomitribe.tribestream.registryng.service.serialization;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.logging.Level;
import java.util.logging.Logger;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class CustomJacksonJaxbJsonProvider extends JacksonJaxbJsonProvider {

    private static final Logger LOGGER = Logger.getLogger(CustomJacksonJaxbJsonProvider.class.getName());

    protected CustomJacksonJaxbJsonProvider(final ObjectMapper mapper) {
        super(SwaggerJsonMapperProducer.lookup(), JacksonJaxbJsonProvider.DEFAULT_ANNOTATIONS);
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
    }

    public CustomJacksonJaxbJsonProvider() {
        this(SwaggerJsonMapperProducer.lookup());
    }

    @Override
    public Object readFrom(Class<Object> type,
                           Type genericType,
                           Annotation[] annotations,
                           MediaType mediaType,
                           MultivaluedMap<String, String> httpHeaders,
                           InputStream entityStream) throws IOException {
        try {
            return super.readFrom(type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Reading entity failed!", e);
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }
    }

    @Override
    public long getSize(Object value, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        try {
            return super.getSize(value, type, genericType, annotations, mediaType);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Getting size for writing entity failed!", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        try {
            return super.isWriteable(type, genericType, annotations, mediaType);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Checking if entity is writable failed!", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void writeTo(Object value,
                        Class<?> type,
                        Type genericType,
                        Annotation[] annotations,
                        MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders,
                        OutputStream entityStream) throws IOException {
        try {
            super.writeTo(value, type, genericType, annotations, mediaType, httpHeaders, entityStream);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Writing entity failed!", e);
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Writing entity failed!", e);
            throw new IOException(e);
        }
    }
}