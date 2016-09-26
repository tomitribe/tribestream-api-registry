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
package org.tomitribe.tribestream.registryng.resources.util;

import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.repository.Repository;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ApplicationWrapperUtil {

    private ApplicationWrapperUtil() {

    }

    public static Swagger shrinkSwagger(final Swagger swagger) {
        Swagger applicationClone = Repository.createShallowCopy(swagger);

        Map<String, Path> paths = applicationClone.getPaths();
        if (paths != null) {
            Map<String, io.swagger.models.Path> shrunkPaths = new HashMap<>();

            for (Map.Entry<String, io.swagger.models.Path> pathEntry : paths.entrySet()) {
                io.swagger.models.Path shrunkPath = new io.swagger.models.Path();
                shrunkPaths.put(pathEntry.getKey(), shrunkPath);
                for (Map.Entry<HttpMethod, Operation> httpMethodOperationEntry : pathEntry.getValue().getOperationMap().entrySet()) {
                    Operation shrunkOperation = new Operation();
                    shrunkOperation.setDescription(httpMethodOperationEntry.getValue().getDescription());
                    shrunkOperation.setSummary(httpMethodOperationEntry.getValue().getSummary());
                    shrunkPath.set(httpMethodOperationEntry.getKey().name().toLowerCase(), shrunkOperation);
                }
            }

            applicationClone.setPaths(shrunkPaths);
        }
        return applicationClone;
    }

    public static Swagger mergeSwagger(final Swagger swagger, final Collection<Endpoint> endpoints) {
        final Swagger result = Repository.createShallowCopy(swagger);
        final HashMap<String, io.swagger.models.Path> newPaths = new HashMap<>();

        if (endpoints != null) {
            for (Endpoint endpoint : endpoints) {
                io.swagger.models.Path newPath = newPaths.get(endpoint.getPath());
                if (newPath == null) {
                    newPath = new io.swagger.models.Path();
                    newPaths.put(endpoint.getPath(), newPath);
                }
                newPath.set(endpoint.getVerb().toLowerCase(), endpoint.getOperation());
            }
        }
        result.setPaths(newPaths);
        return result;
    }

}
