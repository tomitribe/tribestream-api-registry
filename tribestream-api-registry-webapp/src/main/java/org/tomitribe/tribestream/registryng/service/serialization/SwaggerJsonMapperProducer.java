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

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.util.DeserializationModule;
import org.apache.deltaspike.core.api.literal.NamedLiteral;

import javax.enterprise.inject.Produces;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Named;

public class SwaggerJsonMapperProducer {

    public static final String SWAGGER_OBJECT_MAPPER_NAME = "SwaggerObjectMapper";

    @Produces
    @Named(SWAGGER_OBJECT_MAPPER_NAME)
    public ObjectMapper createSwaggerObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new DeserializationModule(true, true));
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);
        return mapper;
    }

    public static ObjectMapper lookup() { // bridge for non CDI contexts ensuring we reuse the CDI instance
        final BeanManager bm = CDI.current().getBeanManager();
        return ObjectMapper.class.cast(bm.getReference(
                bm.resolve(bm.getBeans(ObjectMapper.class, new NamedLiteral(SWAGGER_OBJECT_MAPPER_NAME))),
                ObjectMapper.class, bm.createCreationalContext(null)));
    }
}
