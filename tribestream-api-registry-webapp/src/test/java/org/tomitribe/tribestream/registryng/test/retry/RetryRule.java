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
package org.tomitribe.tribestream.registryng.test.retry;

import lombok.RequiredArgsConstructor;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.tomitribe.tribestream.registryng.test.Registry;

import java.util.function.Supplier;

@RequiredArgsConstructor
public class RetryRule implements TestRule {
    private final Supplier<Registry> registry;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Retry annotation = description.getAnnotation(Retry.class);
                if (annotation == null) {
                    annotation = description.getTestClass().getAnnotation(Retry.class);
                }
                if (annotation != null && annotation.active()) {
                    registry.get().withRetries(() -> {
                        try {
                            base.evaluate();
                        } catch (final Error | RuntimeException e) {
                            throw e;
                        } catch (Throwable throwable) {
                            throw new IllegalStateException(throwable);
                        }
                    }, description.getDisplayName());
                } else {
                    base.evaluate();
                }
            }
        };
    }
}
