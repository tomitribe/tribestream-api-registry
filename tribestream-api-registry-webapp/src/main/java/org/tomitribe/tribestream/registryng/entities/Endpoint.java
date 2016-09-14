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
import org.hibernate.annotations.ForeignKey;
import org.hibernate.envers.Audited;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_VERB_PATH", columnNames = {"verb", "path"})
    }
)
@NamedQueries({
    @NamedQuery(
        name = Endpoint.QRY_FIND_ALL,
        query = "SELECT ep FROM Endpoint ep ORDER BY ep.application.name ASC, ep.application.version DESC, ep.path ASC, ep.verb ASC"),
    @NamedQuery(
        name = Endpoint.QRY_FIND_ALL_WITH_APPLICATION,
        query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
            " ORDER BY ep.application.name ASC, ep.application.version DESC, ep.path ASC, ep.verb ASC"),
    @NamedQuery(
        name = Endpoint.QRY_FIND_BY_APPLICATIONID_VERB_AND_PATH,
        query = "SELECT ep FROM Endpoint ep JOIN FETCH ep.application " +
            " WHERE (concat(ep.application.name, '-', ep.application.version) = :applicationId OR ep.application.id = :applicationId) " +
            "       AND lower(ep.verb) = lower(:verb) AND ep.path = :path")
})
@EntityListeners(OpenAPIDocumentSerializer.class)
@Audited
public class Endpoint extends AbstractEntity {

    public static final String QRY_FIND_ALL = "Endpoint.findAll";

    public static final String QRY_FIND_BY_APPLICATIONID_VERB_AND_PATH = "Endpoint.findByApplicationIdVerbAndPath";

    public static final String QRY_FIND_ALL_WITH_APPLICATION = "Endpoint.findAllWithApplication";

    @ManyToOne(targetEntity = OpenApiDocument.class, optional = false)
    @JoinColumn(name = "APPLICATION_ID", nullable = false)
    @ForeignKey(name = "FK_ENDPOINT_APPLICATION_01")
    private OpenApiDocument application;

    @Column(name = "PATH", nullable = false)
    private String path;

    @Column(name = "VERB", nullable = false)
    private String verb;

    @Column(length = 1024 * 1024)
    @Lob
    private String document;

    private transient Operation operation;

    public OpenApiDocument getApplication() {
        return application;
    }

    public void setApplication(OpenApiDocument application) {
        this.application = application;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getVerb() {
        return verb;
    }

    public void setVerb(String verb) {
        this.verb = verb;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public Operation getOperation() {
        return operation;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }
}
