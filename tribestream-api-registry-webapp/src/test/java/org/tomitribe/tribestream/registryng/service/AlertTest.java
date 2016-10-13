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
package org.tomitribe.tribestream.registryng.service;

import com.icegreen.greenmail.junit.GreenMailRule;
import com.icegreen.greenmail.util.ServerSetup;
import org.apache.openejb.testing.ContainerProperties;
import org.apache.openejb.testing.RandomPort;
import org.apache.openejb.util.NetworkUtil;
import org.apache.tomee.embedded.junit.TomEEEmbeddedSingleRunner;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestRule;
import org.junit.runners.model.Statement;
import org.tomitribe.tribestream.registryng.test.logging.LoggingSetup;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.rules.RuleChain.outerRule;

// to run this test in an IDE don't forget to add
//  -Dtomee.application-composer.application=org.tomitribe.tribestream.registryng.service.AlertTest$App
//
// this is an isolated test running with an intentionnaly broken config to check validations
@Category(AlertTest.class)
public class AlertTest {
    private final GreenMailRule greenMail = new GreenMailRule(new ServerSetup(NetworkUtil.getNextAvailablePort(), "localhost", "smtp"));

    @Rule
    public final TestRule runner = outerRule(greenMail)
            .around((base, description) -> new Statement() { // propagate the server port as a placeholder
                @Override
                public void evaluate() throws Throwable {
                    System.setProperty("test.mail.port", Integer.toString(greenMail.getSmtp().getPort()));
                    try {
                        base.evaluate();
                    } finally {
                        System.getProperties().remove("test.mail.port");
                    }
                }
            })
            .around(new TomEEEmbeddedSingleRunner.Rule(this));

    @Test
    public void run() throws IOException, MessagingException {
        assertTrue(greenMail.waitForIncomingEmail(TimeUnit.MINUTES.toMillis(1), 1));
        final MimeMessage[] messages = greenMail.getReceivedMessages();
        assertEquals(1, messages.length);
        final MimeMessage msg = messages[0];
        assertEquals("no-reply@mock.com", msg.getFrom()[0].toString());
        assertEquals("target@mock.com", msg.getAllRecipients()[0].toString());
        assertTrue(msg.getSubject(), msg.getSubject().startsWith("[ALERT][REGISTRY]"));
        assertEquals(
                "HTTP: KO (One or multiple servers are slow or not responding: http://localhost:9256: no connection)\n" +
                "Database: OK ('select * from OpenApiDocument' suceeds)\n", msg.getContent().toString().replace("\r", ""));
    }

    @ContainerProperties({
            @ContainerProperties.Property(name = "openejb.datasource.plugin.activated", value = "false"),
            @ContainerProperties.Property(name = "hibernate.hbm2ddl.auto", value = "create-drop"),
            @ContainerProperties.Property(name = "registryDatasource", value = "new://Resource?type=DataSource"),
            @ContainerProperties.Property(name = "registryDatasource.JdbcDriver", value = "org.h2.Driver"),
            @ContainerProperties.Property(name = "registryDatasource.JdbcUrl", value = "jdbc:h2:mem:registry-alert;DB_CLOSE_ON_EXIT=FALSE"),
            @ContainerProperties.Property(name = "tribe.registry.elasticsearch.base", value = "http://localhost:9256"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.http.urls", value = "http://localhost:9256"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.period", value = "5 seconds"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.alerter.mail.active", value = "true"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.alerter.mail.to", value = "target@mock.com"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.alerter.mail.from", value = "no-reply@mock.com"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.alerter.mail.host", value = "localhost"),
            @ContainerProperties.Property(name = "tribe.registry.monitoring.alerter.mail.port", value = "${test.mail.port}")
    })
    @TomEEEmbeddedSingleRunner.LifecycleTasks(LoggingSetup.class)
    public static class App {
        @RandomPort("http")
        private int port;
    }
}
