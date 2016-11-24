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
package org.tomitribe.tribestream.registryng.service.client;

import org.apache.openejb.testing.Application;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.tomitribe.auth.signatures.Algorithm;
import org.tomitribe.tribestream.registryng.test.Registry;

import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GenericClientServiceTest {
    @Application
    private Registry registry;

    @Rule
    public final TestRule rule = new TomEEEmbeddedSingleRunner.Rule(this);

    @Inject
    private GenericClientService client;

    @Test
    public void digest() {
        assertEquals("sha-256=n4bQgYhMfWWaL+qgxVrQFaO/TxsrC4Is0V1sFbDwCgg=", client.digestHeader("test", "sha-256"));
    }

    @Test
    public void basic() {
        assertEquals("Basic YTpiNDU2OEBfODclKg==", client.basicHeader("a", "b4568@_87%*"));
    }

    @Test
    public void signature() {
        assertEquals(
                "Signature keyId=\"key\",algorithm=\"hmac-sha256\",headers=\"(request-target) date\",signature=\"OdYumDua8K9Nb/3PZvWdNxbZDTNl33JfObSE4tE/npg=\"",
                client.httpSign(asList("(request-target)", "date"), "POST", "/foo/bar?q=u", "key", "chut", Algorithm.HMAC_SHA256.getJmvName(), new HashMap<String, String>() {{
                    put("date", new Date(0).toString()); // ensure test can be re-executed
                }}));
    }

    @Test
    public void oauth2() {
        assertEquals(
                "bearer awesome-token",
                client.oauth2Header("password", "testuser", "testpassword", null, "client", "client secret", registry.root() + "/api/mock/oauth2/token", false));
    }

    @Test(expected = NullPointerException.class)
    public void badRequest() {
        client.invoke(new GenericClientService.Request());
    }

    @Test
    public void notAuthenticated() {
        final GenericClientService.Request request = new GenericClientService.Request();
        request.setUrl(registry.root() + "/api/spy");
        request.setMethod("GET");

        final GenericClientService.Response response = client.invoke(request);
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response.getStatus());
    }

    @Test
    public void get() throws UnsupportedEncodingException {
        final GenericClientService.Request request = new GenericClientService.Request();
        request.setHeaders(new HashMap<String, String>() {{
            put("test-header", "test");
            put("Authorization", registry.basicHeader());
        }});
        request.setUrl(registry.root() + "/api/spy");
        request.setMethod("GET");

        final GenericClientService.Response response = client.invoke(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getPayload(), response.getPayload().contains("GET/api/spy"));
        assertTrue(response.getPayload(), response.getPayload().contains("test-header=test"));
        assertTrue(response.getPayload(), response.getPayload().contains("authorization=Basic dXRlc3Q6cHRlc3Q="));
        assertEquals("application/octet-stream", response.getHeaders().get("content-type"));
    }

    @Test
    public void post() throws UnsupportedEncodingException {
        final GenericClientService.Request request = new GenericClientService.Request();
        request.setHeaders(new HashMap<String, String>() {{
            put("test-header", "test");
            put("Authorization", registry.basicHeader());
        }});
        request.setUrl(registry.root() + "/api/spy");
        request.setMethod("POST");
        request.setPayload("{\"test\":\"val\"}");

        final GenericClientService.Response response = client.invoke(request);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertTrue(response.getPayload(), response.getPayload().contains("POST/api/spy"));
        assertTrue(response.getPayload(), response.getPayload().contains("test-header=test"));
        assertTrue(response.getPayload(), response.getPayload().contains("authorization=Basic dXRlc3Q6cHRlc3Q="));
        assertTrue(response.getPayload(), response.getPayload().endsWith("{\"test\":\"val\"}"));
        assertEquals("application/octet-stream", response.getHeaders().get("content-type"));
    }
}
