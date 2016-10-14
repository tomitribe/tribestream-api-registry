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

import lombok.extern.java.Log;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.documentation.Description;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.util.Duration;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timeout;
import javax.ejb.Timer;
import javax.ejb.TimerConfig;
import javax.ejb.TimerService;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Event;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Response;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import static java.lang.Boolean.parseBoolean;
import static java.util.Optional.ofNullable;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.ws.rs.core.MediaType.WILDCARD_TYPE;
import static org.apache.deltaspike.core.api.config.ConfigResolver.getPropertyValue;

@Log
@Startup
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
public class MonitoringService {
    @Resource
    private TimerService timerService;

    @Inject
    @Description("How often health checks are executed")
    @ConfigProperty(name = "tribe.registry.monitoring.period", defaultValue = "5 minutes")
    private String period;

    @Inject
    private HTTPMonitoringValidator httpValidator;

    @Inject
    private DatabaseMonitoringValidator mySqlValidator;

    @Inject
    private Event<Alert> alert;

    private Timer timer;
    private long periodMs;

    private volatile boolean running = false;
    private volatile long lastRun = -1;
    private volatile List<ValidationWrapper> last;

    @PostConstruct
    private void boot() {
        if ("skip".equals(period)) {
            log.info("Monitoring is disabled");
            return;
        }

        periodMs = new Duration(period, TimeUnit.SECONDS).getTime(TimeUnit.MILLISECONDS);
        if (periodMs <= 0) {
            log.info("Monitoring is disabled");
            return;
        }

        monitor(null); // init state

        timer = timerService.createIntervalTimer(periodMs, periodMs, new TimerConfig("tribestream-api-registry-monitoring", false));
        log.info("Monitoring active each " + periodMs + "ms");
    }

    @PreDestroy
    private void destroy() {
        ofNullable(timer).ifPresent(Timer::cancel);
    }

    @Timeout
    public void monitor(final Timer ignored) {
        final long now = System.currentTimeMillis();
        if (running) {
            if (now - lastRun > periodMs * 2) {
                log.severe("Seems validation are really slow (taking more than twice the run period: " + period + ")");
            }
            return;
        }
        running = true;

        try {
            final List<ValidationWrapper> validations = Stream.of(httpValidator, mySqlValidator)
                    .map(e -> new ValidationWrapper(e.name(), e.valid()))
                    .collect(toList());
            last = validations;
            final long end = System.currentTimeMillis();
            if (end - now > periodMs) {
                log.warning("Validations take longer than the period: period=" + period + ", validation duration=" + TimeUnit.MILLISECONDS.toSeconds(end - now) + "s");
            }

            if (validations.stream().filter(v -> v.getValidation().getState() == Validation.State.KO).findFirst().isPresent()) {
                onError(validations);
            } else {
                log.fine("All validations suceeded");
            }
        } catch (final Throwable exception) {
            // dont call onError() otherwise it can loop if alerters are buggy
            log.log(SEVERE, "Error running validations", exception);
        } finally {
            lastRun = now;
            running = false;
        }
    }

    private void onError(final Collection<ValidationWrapper> messages) {
        alert.fire(new Alert(messages));
    }

    public Collection<ValidationWrapper> currentHealth() {
        return last;
    }

    public static abstract class MonitoringEntry {
        private boolean active;

        @PostConstruct
        private void init() {
            final String key = "tribe.registry.monitoring." + getClass().getSimpleName().toLowerCase(Locale.ENGLISH).replaceAll("MonitoringValidator", "") + ".active";
            active = parseBoolean(getPropertyValue(key, "true"));
        }

        public boolean isActive() {
            return active;
        }

        public abstract Validation valid();

        public abstract String name();
    }

    @ApplicationScoped
    public static class DatabaseMonitoringValidator extends MonitoringEntry {
        @Inject
        @Description("Timeout to execute select * from OpenApiDocument (limit 1)")
        @ConfigProperty(name = "tribe.registry.monitoring.database.timeout", defaultValue = "10000")
        private Integer timeout;

        @PersistenceContext
        private EntityManager em;

        @Override
        public Validation valid() {
            try { // just ensure we can run a query
                em.createNamedQuery(OpenApiDocument.Queries.FIND_ALL, OpenApiDocument.class)
                        .setHint("javax.persistence.query.timeout", timeout)
                        .setMaxResults(1)
                        .getResultList();
                return new Validation(Validation.State.OK, "'select * from OpenApiDocument' suceeds");
            } catch (final Throwable error) {
                return new Validation(Validation.State.KO, error.getMessage());
            }
        }

        @Override
        public String name() {
            return "Database";
        }
    }

    @ApplicationScoped
    public static class HTTPMonitoringValidator extends MonitoringEntry {
        @Inject
        @Description("URL to check for a 2xx HTTP status (split by a comma)")
        @ConfigProperty(name = "tribe.registry.monitoring.http.urls", defaultValue = "http://localhost:9200")
        private String bases;

        @Inject
        @Description("HTTP receive timeout")
        @ConfigProperty(name = "tribe.registry.monitoring.http.timeout.receive", defaultValue = "10000")
        private String receiveTimeout;

        @Inject
        @Description("HTTP connect timeout")
        @ConfigProperty(name = "tribe.registry.monitoring.http.timeout.connect", defaultValue = "10000")
        private String connectTimeout;

        @Override
        public Validation valid() {
            final Client client = ClientBuilder.newClient()
                    .property("http.connection.timeout", connectTimeout)
                    .property("http.receive.timeout", receiveTimeout)
                    .property("jersey.config.client.connectTimeout", connectTimeout)
                    .property("jersey.config.client.readTimeout", receiveTimeout);
            try {
                final List<Pair<String, Integer>> checks = Stream.of(bases.split(","))
                        .map(base -> ImmutablePair.of(base, doGet(client, base)))
                        .collect(toList());
                if (checks.stream().filter(s -> s.getRight() > 299 || s.getRight() < Response.Status.OK.getStatusCode()).findAny().isPresent()) {
                    return new Validation(Validation.State.KO, "One or multiple servers are slow or not responding: " + checks.stream()
                            .map(p -> p.getLeft() + ": " + (p.getRight() < 0 ? "no connection" : ("HTTP " + p.getRight())))
                            .collect(joining(", ")));
                }
                return new Validation(Validation.State.OK, "Servers are responding");
            } finally {
                client.close();
            }
        }

        @Override
        public String name() {
            return "HTTP";
        }

        private int doGet(final Client client, final String base) {
            try {
                return client.target(base).request(WILDCARD_TYPE).get().getStatus();
            } catch (final WebApplicationException | ProcessingException war) {
                return -1;
            }
        }
    }
}
