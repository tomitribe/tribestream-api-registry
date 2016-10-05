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
package org.tomitribe.tribestream.registryng.domain;

/**
 * This interface contains constants for the names used for the vendor extension
 * in the OpenAPI documents processed by the Tribestream API Registry.
 * <p>A OpenAPI extension with the Tribestream API Registry vendor extension could look like this and this interface
 * contains constants for all proprietary names used.</p>
 * <code>
 * {
 *   "swagger": "2.0",
 *   "info": {},
 *   "paths": {
 *     "pets": {
 *       "get": {
 *         "description": "...",
 *         "x-tribestream-api-registry": {
 *           "status": "ACCEPTED",
 *           "categories": ["mammals"],
 *           "roles": ["roleA", "roleB"],
 *           "auth-methods": ["HTTP Signatures", "Bearer"],
 *           "api-versions": ["0.1"]
 *         }
 *       }
 *     }
 *   }
 * }
 * </code>
 */
public interface TribestreamOpenAPIExtension {

    /**
     * The name of the vendor extension property.
     * The value of this property will be an object that contains all other Tribestream API Registry specific
     * properties.
     */
    public static final String VENDOR_EXTENSION_KEY = "x-tribestream-api-registry";

    /**
     * Contains a string with the status of the endpoint, e.g. <code>"DRAFT"</code> or <code>"ACCEPTED"</code>.
     */
    public static final String PROP_STATUS          = "status";

    /**
     * Contains a list of strings with categories this endpoint belongs to.
     */
    public static final String PROP_CATEGORIES      = "categories";

    /**
     * Contains a list of strings with roles that are allowed to invoke this endpoint.
     */
    public static final String PROP_ROLES           = "roles";

    /**
     * Contains a list of strings with authentication methods supported for this endpoint.
     */
    public static final String PROP_AUTH_METHODS    = "auth-methods";

    public static final String PROP_API_VERSIONS    = "api-versions";

    public static final String HUMAN_READABLE_PATH           = "human-readable-path";
}
