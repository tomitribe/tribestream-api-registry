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
package com.tomitribe.tribestream.registryng.resources;

import org.apache.openejb.jee.JaxbJavaee;
import org.apache.openejb.jee.WebApp;
import org.apache.openejb.jee.jpa.unit.Persistence;
import org.apache.openejb.jee.jpa.unit.PersistenceUnit;
import org.apache.openejb.testing.Configuration;
import org.apache.openejb.testing.Module;
import org.apache.openejb.testng.PropertiesBuilder;
import org.apache.openejb.util.Join;
import org.apache.openejb.util.NetworkUtil;
import org.hibernate.ejb.packaging.NamedInputStream;
import org.hibernate.ejb.packaging.Scanner;

import java.io.FileInputStream;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collections;
import java.util.Properties;
import java.util.Set;

public abstract class AbstractResourceTest {

    @Module
    public PersistenceUnit jpa() throws Exception {
        try (final InputStream fis = new FileInputStream("src/main/resources/META-INF/persistence.xml")) {
            final PersistenceUnit unit = Persistence.class.cast(JaxbJavaee.unmarshal(Persistence.class, fis, false)).getPersistenceUnit().iterator().next();
            unit.getProperties().clear();
            unit.setProperty("hibernate.hbm2ddl.auto", "create-drop");
            unit.setProperty("hibernate.ejb.resource_scanner", NoScanning.class.getName()); // we run in parallel so folders can be visited and deleted in parallel
            return unit;
        }
    }

    public static class NoScanning implements Scanner {
        @Override
        public Set<Package> getPackagesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
            return Collections.emptySet();
        }

        @Override
        public Set<Class<?>> getClassesInJar(URL jartoScan, Set<Class<? extends Annotation>> annotationsToLookFor) {
            return Collections.emptySet();
        }

        @Override
        public Set<NamedInputStream> getFilesInJar(URL jartoScan, Set<String> filePatterns) {
            return Collections.emptySet();
        }

        @Override
        public Set<NamedInputStream> getFilesInClasspath(Set<String> filePatterns) {
            return Collections.emptySet();
        }

        @Override
        public String getUnqualifiedJarName(URL jarUrl) {
            return null;
        }
    }
}
