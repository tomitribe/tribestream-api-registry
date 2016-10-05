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
package org.tomitribe.tribestream.registryng.service;

import java.util.stream.Stream;

import static java.util.stream.Collectors.joining;

public final class PathTransformUtil {

    private PathTransformUtil() {

    }

    public static String bracesToColon(String openApiTemplate) { // TODO: what's wrong with openApiTemplate.replaceAll("\\{(\\w+)\\}", ":$1")
        // The root path is the most simple case
        if (openApiTemplate.equals("/")) {
            return openApiTemplate;
        }

        return Stream.of(openApiTemplate.split("/"))
                .map(part ->
                    part.startsWith("{") && part.endsWith("}")
                            ? ":" + part.substring(1, part.length() - 1)
                            : part)
                .collect(joining("/"));
    }
}
