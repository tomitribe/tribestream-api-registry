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
package org.tomitribe.tribestream.registryng.entities;

import io.swagger.models.Operation;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.envers.Audited;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.ForeignKey;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.io.IOException;

import static org.tomitribe.tribestream.registryng.entities.Normalizer.normalize;

@Entity
@Table(
        uniqueConstraints = {
                @UniqueConstraint(name = "UQ_VERB_PATH", columnNames = {"verb", "path"})
        }
)
@NamedQueries({
        @NamedQuery(
                name = Endpoint.Queries.FIND_ALL,
                query = "SELECT ep FROM Endpoint ep ORDER BY ep.application.name ASC, ep.application.version DESC, ep.path ASC, ep.verb ASC"),
        @NamedQuery(
                name = Endpoint.Queries.FIND_ALL_WITH_APPLICATION,
                query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
                        " ORDER BY ep.application.name ASC, ep.application.version DESC, ep.path ASC, ep.verb ASC"),
        @NamedQuery(
                name = Endpoint.Queries.FIND_BY_APPLICATIONID_VERB_AND_PATH,
                query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
                        " WHERE (concat(ep.application.name, '-', ep.application.version) = :applicationId OR ep.application.id = :applicationId) " +
                        "       AND lower(ep.verb) = lower(:verb) AND ep.path = :path"),
        @NamedQuery(
                name = Endpoint.Queries.FIND_BY_HUMAN_REDABLE_PATH,
                query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
                        "WHERE ep.humanReadablePath = :endpointPath AND ep.application.humanReadableName = :applicationName AND " +
                        "lower(ep.verb) = lower(:verb) AND ep.application.version = :applicationVersion"),
        @NamedQuery(
                name = Endpoint.Queries.FIND_BY_HUMAN_REDABLE_PATH_NO_VERSION,
                query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
                        "WHERE ep.humanReadablePath = :endpointPath AND ep.application.humanReadableName = :applicationName AND " +
                        "lower(ep.verb) = lower(:verb) " +
                        "ORDER BY ep.application.version")
})
@EntityListeners(OpenAPIDocumentSerializer.class)
@Audited
@Getter
@Setter
@ToString(of = {"verb", "path"})
public class Endpoint extends AbstractEntity {
    public interface Queries {
        String FIND_ALL = "Endpoint.findAll";
        String FIND_BY_APPLICATIONID_VERB_AND_PATH = "Endpoint.findByApplicationIdVerbAndPath";
        String FIND_ALL_WITH_APPLICATION = "Endpoint.findAllWithApplication";
        String FIND_BY_HUMAN_REDABLE_PATH = "Endpoint.findByHumanReadablePath";
        String FIND_BY_HUMAN_REDABLE_PATH_NO_VERSION = "Endpoint.findByHumanReadablePathWithoutVersion";
    }

    @ManyToOne(targetEntity = OpenApiDocument.class, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false, foreignKey = @ForeignKey(name = "FK_ENDPOINT_APPLICATION_01"))
    private OpenApiDocument application;

    @Column(name = "PATH", nullable = false)
    private String path;

    @Column(name = "VERB", nullable = false)
    private String verb;

    @Column(length = 1024 * 1024)
    @Lob
    private String document;

    @Column(name = "HUMAN_READABLE_PATH", nullable = false)
    private String humanReadablePath; // stored in case we make it editable

    private transient Operation operation;

    @PrePersist
    @PreUpdate
    public void updateHumanReadblePath() {
        humanReadablePath = normalize(path);
        if (humanReadablePath.startsWith("/")) {
            humanReadablePath = humanReadablePath.substring(1);
        }
    }

    public Operation getOperation() {
        try {
            return operation == null ? (operation = SwaggerJsonMapperProducer.lookup().readValue(document, Operation.class)) : operation;
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
