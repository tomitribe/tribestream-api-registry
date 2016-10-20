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
package org.tomitribe.tribestream.registryng.security.oauth2;

import lombok.Getter;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.documentation.Description;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

@ApplicationScoped
@Getter
public class Oauth2Configuration {

    @Inject
    @Description("If the token is a JWT the username attribute in the payload")
    @ConfigProperty(name = "tribe.registry.oauth2.jwt.username", defaultValue = "username")
    private String jwtUsernameAttribute;

    @Inject
    @Description("OAuth2 endpoint")
    @ConfigProperty(name = "tribe.registry.oauth2.authorizationServerUrl")
    private String authServerUrl;

    @Inject
    @Description("OAuth2 client id if used")
    @ConfigProperty(name = "tribe.registry.oauth2.clientId")
    private String clientId;

    @Inject
    @Description("OAuth2 client secret if client id is set")
    @ConfigProperty(name = "tribe.registry.oauth2.clientSecret")
    private String clientSecret;

    @Inject
    @Description("OAuth2 HTTP TLS protocol supported")
    @ConfigProperty(name = "tribe.registry.oauth2.tlsProtocol", defaultValue = "TLSv1.2")
    private String tlsProtocol;

    @Inject
    @Description("OAuth2 HTTP TLS provider")
    @ConfigProperty(name = "tribe.registry.oauth2.tlsProvider")
    private String tlsProvider;

    @Inject
    @Description("OAuth2 truststore")
    @ConfigProperty(name = "tribe.registry.oauth2.trustStore")
    private String trustStoreFileName;

    @Inject
    @Description("OAuth2 truststore type")
    @ConfigProperty(name = "tribe.registry.oauth2.trustStoreType")
    private String trustStoreType;

}
