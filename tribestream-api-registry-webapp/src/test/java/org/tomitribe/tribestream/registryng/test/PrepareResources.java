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
package org.tomitribe.tribestream.registryng.test;

import org.apache.openejb.loader.Files;
import org.apache.openejb.loader.IO;
import org.apache.tomee.embedded.LifecycleTask;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.stream.Stream;

import static org.junit.Assert.fail;

// our webapp resource build doesn't respect much docbase philosophy so let's fake one
public class PrepareResources implements LifecycleTask {
    @Override
    public Closeable beforeContainerStartup() {
        final File out = new File("target/tests-webapp/");
        Files.deleteOnExit(out);
        Stream.of("target/static-resources", "src/main/webapp")
                .forEach(s -> {
                    try {
                        IO.copyDirectory(new File(s), out);
                    } catch (final IOException e) {
                        fail(e.getMessage());
                    }
                });
        return () -> {
        };
    }
}
