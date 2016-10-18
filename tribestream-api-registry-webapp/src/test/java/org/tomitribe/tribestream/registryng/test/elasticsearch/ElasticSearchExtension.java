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
package org.tomitribe.tribestream.registryng.test.elasticsearch;

import org.jboss.arquillian.container.spi.event.container.AfterUnDeploy;
import org.jboss.arquillian.container.spi.event.container.BeforeDeploy;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;

import java.util.Properties;

import static java.lang.System.getProperty;

public class ElasticSearchExtension implements LoadableExtension {

    private ElasticsearchServer elasticsearch;

    /**
     * Add the ElasticSearchLifecycle as an event observer to the Arquillian ExtensionBuilder
     *
     * @param builder ExtensionBuilder
     */
    @Override
    public void register(final ExtensionBuilder builder) {
        builder.observer(ElasticSearchExtension.class);
    }

    public void executeBeforeDeploy(@Observes final BeforeDeploy event, final TestClass testClass) throws Exception {

        elasticsearch = new ElasticsearchServer(getProperty("elasticsearch.test.workdir"), getProperty("elasticsearch.test.version", "2.4.1"))
                // TODO: Make it work with dynamic ports. Problem: Arquillian extension is running in different VM
                .start(Integer.parseInt(System.getProperty("es.http.port", "9205")), Integer.parseInt(System.getProperty("es.transport.tcp.port", "9305")));
    }

    public void executeAfterUnDeploy(@Observes final AfterUnDeploy event, final TestClass testClass) throws Exception {
        elasticsearch.close();
    }
}

