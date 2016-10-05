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
package org.tomitribe.tribestream.registryng.resources.processor;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.tomitribe.tribestream.registryng.domain.ApplicationWrapper;
import org.tomitribe.tribestream.registryng.domain.TribestreamOpenAPIExtension;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.repository.Repository;

import javax.enterprise.context.ApplicationScoped;
import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@ApplicationScoped
public class ApplicationProcessor {
    public ApplicationWrapper toWrapper(final OpenApiDocument application) {
        final Swagger reducedSwagger = shrinkSwagger(mergeSwagger(application.getSwagger(), application.getEndpoints()));
        return new ApplicationWrapper(reducedSwagger, application.getHumanReadableName());
    }

    private Swagger shrinkSwagger(final Swagger swagger) {
        final Swagger applicationClone = Repository.createShallowCopy(swagger);

        final Map<String, Path> paths = applicationClone.getPaths();
        if (paths != null) {
            final Map<String, io.swagger.models.Path> shrunkPaths = new HashMap<>();
            for (final Map.Entry<String, io.swagger.models.Path> pathEntry : paths.entrySet()) {
                io.swagger.models.Path shrunkPath = new io.swagger.models.Path();
                shrunkPaths.put(pathEntry.getKey(), shrunkPath);
                for (Map.Entry<HttpMethod, Operation> httpMethodOperationEntry : pathEntry.getValue().getOperationMap().entrySet()) {
                    Operation shrunkOperation = new Operation();
                    shrunkOperation.setDescription(httpMethodOperationEntry.getValue().getDescription());
                    shrunkOperation.setSummary(httpMethodOperationEntry.getValue().getSummary());
                    shrunkPath.set(httpMethodOperationEntry.getKey().name().toLowerCase(Locale.ENGLISH), shrunkOperation);
                }
            }

            applicationClone.setPaths(shrunkPaths);
        }
        return applicationClone;
    }

    private Swagger mergeSwagger(final Swagger swagger, final Collection<Endpoint> endpoints) {
        final Swagger result = Repository.createShallowCopy(swagger);
        final HashMap<String, io.swagger.models.Path> newPaths = new HashMap<>();
        if (endpoints != null) {
            for (final Endpoint endpoint : endpoints) {
                io.swagger.models.Path newPath = newPaths.get(endpoint.getPath());
                if (newPath == null) {
                    newPath = new io.swagger.models.Path();
                    newPaths.put(endpoint.getPath(), newPath);
                }

                if (endpoint.getOperation().getVendorExtensions() == null) {
                    endpoint.getOperation().setVendorExtension(TribestreamOpenAPIExtension.VENDOR_EXTENSION_KEY, new HashMap<>());
                }
                final Map<String, Object> appExt = Map.class.cast(endpoint.getOperation().getVendorExtensions());
                appExt.put(TribestreamOpenAPIExtension.HUMAN_READABLE_PATH, endpoint.getHumanReadablePath()); // for navigation

                newPath.set(endpoint.getVerb().toLowerCase(Locale.ENGLISH), endpoint.getOperation());
            }
        }
        result.setPaths(newPaths);
        return result;
    }
}
