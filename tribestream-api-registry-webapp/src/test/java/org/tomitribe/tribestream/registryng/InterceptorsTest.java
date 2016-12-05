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
package org.tomitribe.tribestream.registryng;

import org.junit.Assert;
import org.junit.Test;
import org.tomitribe.tribestream.registryng.elasticsearch.ClientCallInterceptor;
import org.tomitribe.tribestream.registryng.elasticsearch.UnreachableException;
import org.tomitribe.tribestream.registryng.resources.WithElasticSearchCallInterceptor;

import javax.interceptor.InvocationContext;
import javax.ws.rs.core.Response;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.ConnectException;
import java.util.Map;

public class InterceptorsTest {

    @Test
    public void it_should_catch_ConnectException() throws Exception {
        ClientCallInterceptor interceptor = new ClientCallInterceptor();
        // simple call that throws the exception right away
        try {
            interceptor.onCall(new DummyInvocationContext() {
                @Override
                public Object proceed() throws Exception {
                    throw new ConnectException();
                }
            });
        } catch (Exception e) {
            if (!UnreachableException.class.isInstance(e)) {
                Assert.fail("Exception not expected here");
                e.printStackTrace();
            }
        }
        // call that throws the exception a few levels away
        try {
            interceptor.onCall(new DummyInvocationContext() {
                @Override
                public Object proceed() throws Exception {
                    throw new Exception(new Exception(new ConnectException()));
                }
            });
        } catch (Exception e) {
            if (!UnreachableException.class.isInstance(e)) {
                Assert.fail("Exception not expected here");
                e.printStackTrace();
            }
        }
        // simple call that throws the OTHER exception right away
        try {
            interceptor.onCall(new DummyInvocationContext() {
                @Override
                public Object proceed() throws Exception {
                    throw new OtherException(null);
                }
            });
        } catch (OtherException e) {
            // expected
        }
        // call that throws the OTHER exception a few levels away
        try {
            interceptor.onCall(new DummyInvocationContext() {
                @Override
                public Object proceed() throws Exception {
                    throw new OtherException(new Exception(new Exception()));
                }
            });
        } catch (OtherException e) {
            // expected
        }
    }

    @Test
    public void it_should_catch_UnreachableException() throws Exception {
        WithElasticSearchCallInterceptor interceptor = new WithElasticSearchCallInterceptor();
        // no exception
        Assert.assertEquals("ok", interceptor.onCall(new DummyInvocationContext() {
            @Override
            public Object proceed() throws Exception {
                return "ok";
            }
        }));
        // with exception
        Response resp = (Response) interceptor.onCall(new DummyInvocationContext() {
            @Override
            public Object proceed() throws Exception {
                throw new Exception(new UnreachableException(null));
            }
        });
        Assert.assertEquals("{\"key\": \"elasticsearch.unavailable.exception\"}", resp.getEntity());
    }

    private class OtherException extends Exception {
        public OtherException(Throwable cause) {
            super(cause);
        }
    }

    private class DummyInvocationContext implements InvocationContext {

        @Override
        public Object getTarget() {
            return null;
        }

        @Override
        public Method getMethod() {
            return null;
        }

        @Override
        public Constructor<?> getConstructor() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public void setParameters(Object[] objects) {

        }

        @Override
        public Map<String, Object> getContextData() {
            return null;
        }

        @Override
        public Object proceed() throws Exception {
            return null;
        }

        @Override
        public Object getTimer() {
            return null;
        }
    }
}
