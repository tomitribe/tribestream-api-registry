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
package org.tomitribe.tribestream.registryng.test.selenium;

import org.apache.openejb.util.JarExtractor;
import org.apache.tomee.embedded.LifecycleTask;
import org.openqa.selenium.phantomjs.PhantomJSDriver;
import org.openqa.selenium.phantomjs.PhantomJSDriverService;
import org.openqa.selenium.remote.DesiredCapabilities;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static java.util.Optional.ofNullable;
import static org.apache.ziplock.JarLocation.jarFromRegex;

// small wrapper creating a phantomjs driver instance and managing its lifecycle
// note that with 7.0.2 upgrade we will get right of the singleton and use in the Registry class:
//
// @TomEEEmbeddedApplicationRunner.LifecycleTask
// private PhantomJsLifecycle.Task phantomjsLifecycle;
//
public class PhantomJsLifecycle {
    public static final PhantomJsLifecycle SINGLETON = new PhantomJsLifecycle("target/Registry-phantomjs-" + System.nanoTime());

    private final String work;
    private PhantomJSDriverService service;
    private PhantomJSDriver driver;

    private PhantomJsLifecycle(final String work) {
        this.work = work;
    }

    public PhantomJSDriver getDriver() {
        ensureServiceExists();
        return driver == null ? (driver = new PhantomJSDriver(service, DesiredCapabilities.chrome())) : driver;
    }

    private void ensureServiceExists() {
        if (service != null) {
            return;
        }

        final File phantomJs = new File(work, "phantomjs");
        try {
            JarExtractor.extract(jarFromRegex("arquillian-phantom-binary.*" + findClassifier() + ".jar"), phantomJs);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        final File exec = new File(phantomJs, "bin/phantomjs" + (System.getProperty("os.name").toLowerCase(Locale.ENGLISH).contains("win") ? ".exe" : ""));
        if (!exec.isFile()) {
            throw new IllegalStateException("Didn't find phantomjs executable in " + phantomJs);
        }
        exec.setExecutable(true); // ignore returned value for platform ignoring that

        service = new PhantomJSDriverService.Builder()
                .withLogFile(new File(work, "ghostdriver.log"))
                .usingPhantomJSExecutable(exec)
                .usingAnyFreePort()
                .build();
    }

    private static String findClassifier() {
        final String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (os.contains("mac")) {
            return "macosx";
        }
        if (os.contains("win")) {
            return "windows";
        }
        if (os.contains("linux")) {
            return "linux-64";
        }
        return ""; // fine if a single impl is there
    }

    public static class Task implements LifecycleTask {
        @Override
        public Closeable beforeContainerStartup() { // allows to ensure we close it with the container automatically
            return () -> {
                try {
                    ofNullable(SINGLETON.driver).ifPresent(PhantomJSDriver::close);
                } finally {
                    ofNullable(SINGLETON.service).ifPresent(PhantomJSDriverService::stop);
                }
            };
        }
    }
}
