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
package org.tomitribe.tribestream.registryng.service.monitoring;

import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.documentation.Description;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Properties;

@ApplicationScoped
public class MailAlerter {
    @Inject
    @Description("Is mail alerting active")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.active", defaultValue = "false")
    private volatile Boolean active;

    @Inject
    @Description("Mail transport")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.transport", defaultValue = "smtp")
    private String transport;

    @Inject
    @Description("Mail user if authentication is required")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.user")
    private String user;

    @Inject
    @Description("Mail password if authentication is required")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.password")
    private String password;

    @Inject
    @Description("Mail server host")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.host")
    private String host;

    @Inject
    @Description("Mail server port")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.port")
    private Integer port;

    @Inject
    @Description("Mail sending timeout")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.timeout", defaultValue = "0")
    private Integer timeout;

    @Inject
    @Description("Is tls needed")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.tls", defaultValue = "false")
    private Boolean tls;

    @Inject
    @Description("Is authentication required (automatic if password is provided)")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.auth", defaultValue = "false")
    private Boolean auth;

    @Inject
    @Description("Mail recipient for alerts")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.to")
    private String to;

    @Inject
    @Description("Mail from value")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.from")
    private String from;

    @Inject
    @Description("Subject of the mail, ${hostname} and ${date} can be used as templates")
    @ConfigProperty(name = "tribe.registry.monitoring.alerter.mail.subject", defaultValue = "[ALERT][REGISTRY][${hostname}][${date}]")
    private String subjectTemplate;

    private String hostname;
    private Session session;

    @PostConstruct
    private void init() {
        active = active && to != null;
        if (!active) {
            return;
        }

        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (final UnknownHostException e) {
            hostname = "unknown";
        }

        final Properties properties = new Properties();
        properties.setProperty("mail.transport.protocol", transport);
        properties.setProperty("mail." + transport + ".host", host);
        properties.setProperty("mail." + transport + ".port", Integer.toString(port));
        if (tls) {
            properties.setProperty("mail." + transport + ".starttls.enable", "true");
        }
        if (user != null) {
            properties.setProperty("mail." + transport + ".user", user);
        }
        if (password != null) {
            properties.setProperty("password", password);
        }
        if (auth || password != null) {
            properties.setProperty("mail." + transport + ".auth", "true");
        }
        if (timeout > 0) {
            properties.setProperty("mail." + transport + ".timeout", Integer.toString(timeout));
        }

        if (password != null) {
            final PasswordAuthentication pa = new PasswordAuthentication(user, password);
            session = Session.getInstance(properties, new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return pa;
                }
            });
        } else {
            session = Session.getInstance(properties);
        }
    }

    @PreDestroy
    private void stop() {
        active = false;
    }

    public void onAlert(@Observes final Alert alert) {
        if (!active) {
            return;
        }
        try {
            sendMail(alert);
        } catch (final MessagingException e) {
            throw new IllegalStateException(e);
        }
    }

    private void sendMail(final Alert alert) throws MessagingException {
        final String subject = StrSubstitutor.replace(subjectTemplate, new HashMap<String, String>() {{
            put("hostname", hostname);
            put("date", LocalDateTime.now().toString());
        }});
        final String body = alert.text();

        final MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(from));
        message.addRecipient(Message.RecipientType.TO, new InternetAddress(to));
        message.setSubject(subject);
        message.setText(body);
        Transport.send(message);
    }
}
