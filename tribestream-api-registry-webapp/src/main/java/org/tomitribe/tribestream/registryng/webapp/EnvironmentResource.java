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
package org.tomitribe.tribestream.registryng.webapp;

import org.tomitribe.tribestream.registryng.Version;
import org.apache.openejb.util.OpenEjbVersion;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("server")
public class EnvironmentResource {
    private final Environment environment = new Environment(OpenEjbVersion.get().getVersion(), Version.VERSION);

    @GET
    @Path("info")
    public Environment server() {
        return environment;
    }

    public static class Environment {
        private String serverVersion;
        private String applicationVersion;

        public Environment() {
            // no-op
        }

        public Environment(final String serverVersion, final String applicationVersion) {
            this.serverVersion = serverVersion;
            this.applicationVersion = applicationVersion;
        }

        public String getServerVersion() {
            return serverVersion;
        }

        public void setServerVersion(final String serverVersion) {
            this.serverVersion = serverVersion;
        }

        public String getApplicationVersion() {
            return applicationVersion;
        }

        public void setApplicationVersion(final String applicationVersion) {
            this.applicationVersion = applicationVersion;
        }
    }
}