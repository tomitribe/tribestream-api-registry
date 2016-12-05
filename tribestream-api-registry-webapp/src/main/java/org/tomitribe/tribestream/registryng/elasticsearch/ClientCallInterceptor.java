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
package org.tomitribe.tribestream.registryng.elasticsearch;

import javax.interceptor.AroundInvoke;
import javax.interceptor.Interceptor;
import javax.interceptor.InvocationContext;
import java.net.ConnectException;

@Interceptor
@ClientCall
/**
 * It wraps method calls that use the elasticsearch remote client. In case we have a ConnectException we should let
 * the system know that this is related to elastic search only. Throw custom
 * org.tomitribe.tribestream.registryng.elasticsearch.UnreachableException
 */
public class ClientCallInterceptor {

    // It can be deep in the stacktrace. Check if the target exception is in there.
    private boolean isConnectException(Throwable original) {
        if (ConnectException.class.isInstance(original)) {
            return true;
        }
        Throwable cause = original.getCause();
        if (cause == null || original == cause) {
            return false;
        }
        return isConnectException(cause);
    }

    @AroundInvoke
    public Object onCall(InvocationContext ctx) throws Exception {
        try {
            return ctx.proceed();
        } catch (Throwable original) {
            if (isConnectException(original)) {
                throw new UnreachableException(original);
            }
            throw original;
        }
    }

}
