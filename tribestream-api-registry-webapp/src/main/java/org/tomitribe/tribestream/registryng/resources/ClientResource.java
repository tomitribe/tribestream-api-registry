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
package org.tomitribe.tribestream.registryng.resources;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.tomitribe.tribestream.registryng.service.client.GenericClientService;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;

@Path("try")
@ApplicationScoped
public class ClientResource {
    @Inject
    private GenericClientService service;

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public HttpResponse invoke(final HttpRequest request) {
        try {
            final GenericClientService.Request req = new GenericClientService.Request();

            req.setMethod(request.getMethod());
            req.setUrl(request.getUrl());
            req.setPayload(request.getPayload());
            req.setIgnoreSsl(request.isIgnoreSsl());
            req.setHeaders(new HashMap<>(ofNullable(request.getHeaders()).orElse(emptyMap())));

            ofNullable(request.getDigest())
                    .filter(o -> o.getAlgorithm() != null)
                    .ifPresent(o -> {
                        if (req.getHeaders().put(
                                ofNullable(o.getHeader()).orElse("Digest"),
                                service.digestHeader(ofNullable(request.getPayload()).orElse(""), o.getAlgorithm())) != null) {
                            throw new IllegalArgumentException("You already have a " + o.getHeader() + " header, oauth2 would overwrite it, please fix the request");
                        }
                    });


            final GenericClientService.Response response = service.invoke(req);
            return new HttpResponse(response.getStatus(), response.getHeaders(), response.getPayload(), null);
        } catch (final RuntimeException re) {
            return new HttpResponse(-1, emptyMap(), null, re.getMessage() /*TODO: analyze it?*/);
        }
    }

    @POST
    @Path("header/oauth2")
    public ComputedHeader getOAuth2(final OAuth2Header request, @QueryParam("ignore-ssl") @DefaultValue("false") final boolean ignoreSsl) {
        return ofNullable(request)
                .filter(o -> o.getUsername() != null || o.getRefreshToken() != null)
                .map(o -> new ComputedHeader(request.getHeader(), service.oauth2Header(
                        o.getGrantType(), o.getUsername(), o.getPassword(), o.getRefreshToken(), o.getClientId(), o.getClientSecret(),
                        o.getEndpoint(), ignoreSsl)))
                .orElseThrow(() -> new WebApplicationException(Response.Status.BAD_REQUEST));
    }

    @POST
    @Path("header/basic")
    public ComputedHeader getBasic(final BasicHeader request) {
        return ofNullable(request)
                .filter(b -> b.getUsername() != null)
                .map(o -> new ComputedHeader(request.getHeader(), service.basicHeader(o.getUsername(), o.getPassword())))
                .orElseThrow(() -> new WebApplicationException(Response.Status.BAD_REQUEST));
    }

    @POST
    @Path("header/signature")
    public ComputedHeader getSignature(final HttpSignatureHeader request) {
        return ofNullable(request) // should be the last one cause can depend on other headers potentially
                .filter(o -> o.getHeaders() != null && !o.getHeaders().isEmpty() && o.getAlias() != null && o.getSecret() != null)
                .map(o -> {
                    final URL url;
                    try {
                        url = new URL(request.getUrl());
                    } catch (final MalformedURLException e) {
                        throw new IllegalArgumentException(e);
                    }
                    return new ComputedHeader(
                            request.getHeader(),
                            service.httpSign(
                                    ofNullable(o.getHeaders()).orElseGet(() -> singletonList("(request-target)")), request.getMethod(),
                                    url.getPath() + ofNullable(url.getQuery()).filter(q -> q != null && !q.isEmpty()).map(q -> "?" + q).orElse(""),
                                    o.getAlias(), o.getSecret(),
                                    ofNullable(o.getAlgorithm()).orElse("hmac-sha256"),
                                    request.getRequestHeaders()));
                })
                .orElseThrow(() -> new WebApplicationException(Response.Status.BAD_REQUEST));
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ComputedHeader {
        private String name;
        private String value;
    }

    @Data
    public static class OAuth2Header {
        private String header;
        private String grantType;
        private String username;
        private String password;
        private String refreshToken;
        private String clientId;
        private String clientSecret;
        private String endpoint;
    }

    @Data
    public static class HttpSignatureHeader {
        private String header;
        private String method;
        private String url;
        private Map<String, String> requestHeaders;
        private List<String> headers;
        private String algorithm;
        private String alias;
        private String secret;
    }

    @Data
    public static class BasicHeader {
        private String header;
        private String username;
        private String password;
    }

    @Data
    public static class DigestHeader {
        private String header;
        private String algorithm;
    }

    @Data
    public static class HttpRequest {
        private boolean ignoreSsl;
        private String method;
        private String url;
        private Map<String, String> headers;
        private DigestHeader digest;
        private String payload;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class HttpResponse {
        private int status;
        private Map<String, String> headers;
        private String payload;
        private String error;
    }
}
