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
package org.tomitribe.tribestream.registryng.repository.orientdb;

import com.orientechnologies.orient.core.db.ODatabaseRecordThreadLocal;
import com.orientechnologies.orient.core.sql.query.OSQLSynchQuery;
import com.orientechnologies.orient.object.db.OObjectDatabaseTx;
import com.orientechnologies.orient.object.enhancement.OObjectMethodFilter;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.apache.deltaspike.core.api.config.ConfigProperty;
import org.tomitribe.tribestream.registryng.repository.orientdb.model.SwaggerReference;
import org.tomitribe.tribestream.registryng.security.LoginContext;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;

@ApplicationScoped
public class OrientDbRepository {
    @Inject
    private LoginContext loginContext;

    @Inject
    @ConfigProperty(name = "tribe.registry.orientdb.user", defaultValue = "root")
    private String user;

    @Inject
    @ConfigProperty(name = "tribe.registry.orientdb.password")
    private String password;

    @Inject
    @ConfigProperty(name = "tribe.registry.orientdb.url", defaultValue = "memory:tribestream-api-registry")
    private String url;

    private OObjectDatabaseTx db;

    @PostConstruct
    private void start() { // TODO: thread safety
        db = new OObjectDatabaseTx(url);
        if (url.startsWith("remote:")) {
            db.open(user, password);
        } else if (url.startsWith("memory:")) {
            db.create();
        }
        db.registerClassMethodFilter(Path.class, new OObjectMethodFilter() {
            private final Collection<String> excluded = Stream.of("set", "get").collect(toSet());

            @Override
            public boolean isHandled(final Method m) {
                return !excluded.contains(m.getName()) && super.isHandled(m);
            }
        });
        db.getEntityManager().registerEntityClasses(Swagger.class.getPackage().getName());
        db.getEntityManager().registerEntityClass(SwaggerReference.class);
        ODatabaseRecordThreadLocal.INSTANCE.remove();
    }

    public SwaggerReference save(final SwaggerReference swagger) {
        return on(() -> db.detachAll(db.save(swagger), true));
    }

    public SwaggerReference delete(final SwaggerReference swagger) {
        return on(() -> {
            final List<SwaggerReference> found = db.query(new OSQLSynchQuery<Swagger>("select * from SwaggerReference where id = ?"), swagger.getId());
            final SwaggerReference next = found.iterator().next();
            final SwaggerReference swaggerReference = db.detachAll(next, true);
            db.delete(next);
            return swaggerReference;
        });
    }

    public SwaggerReference find(final String id) {
        return on(() -> db.query(new OSQLSynchQuery<Swagger>("select * from SwaggerReference where id = ?"), id).stream()
                .map(s -> SwaggerReference.class.cast(db.detachAll(s, true)))
                .findFirst()
                .orElse(null));
    }

    private <T> T on(final Supplier<T> run) {
        db.activateOnCurrentThread();
        try {
            return run.get();
        } finally {
            ODatabaseRecordThreadLocal.INSTANCE.remove();
        }
    }

    @PreDestroy
    private void stop() {
        if (!db.isClosed()) { // shutdown hook already executed
            db.activateOnCurrentThread().close();
        }
    }
}
