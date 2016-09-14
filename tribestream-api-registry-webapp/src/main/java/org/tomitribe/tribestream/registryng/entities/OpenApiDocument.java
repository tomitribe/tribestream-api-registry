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

import io.swagger.models.Swagger;
import org.hibernate.envers.Audited;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityListeners;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.ArrayList;
import java.util.Collection;

@Entity
@Table(
    uniqueConstraints = {
        @UniqueConstraint(name = "UQ_NAME_VERSION", columnNames = {"name", "version"})
    }
)
@NamedQueries({
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_BY_NAME_AND_VERSION,
        query = "SELECT d FROM OpenApiDocument d WHERE d.name = :name AND d.version = :version"),
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_BY_APPLICATIONID,
        query = "SELECT d FROM OpenApiDocument d WHERE d.id = :applicationId"),
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_BY_APPLICATIONID_WITH_ENDPOINTS,
        query = "SELECT DISTINCT d FROM OpenApiDocument d LEFT JOIN FETCH d.endpoints WHERE d.id = :applicationId"),
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_BY_NAME,
        query = "SELECT d FROM OpenApiDocument d WHERE d.name = :name ORDER BY d.version DESC"),
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_ALL,
        query = "SELECT d FROM OpenApiDocument d ORDER BY d.name ASC, d.version DESC"),
    @NamedQuery(
        name = OpenApiDocument.QRY_FIND_ALL_WITH_ENDPOINTS,
        query = "SELECT DISTINCT d FROM OpenApiDocument d JOIN FETCH d.endpoints ORDER BY d.name ASC, d.version DESC")
})
@EntityListeners(OpenAPIDocumentSerializer.class)
@Audited
public class OpenApiDocument extends AbstractEntity {

    public static final String QRY_FIND_BY_NAME_AND_VERSION = "OpenApiDocument.findByNameAndVersion";

    public static final String QRY_FIND_BY_NAME = "OpenApiDocument.findByName";

    public static final String QRY_FIND_ALL = "OpenApiDocument.findAll";

    public static final String QRY_FIND_ALL_WITH_ENDPOINTS = "OpenApiDocument.findAllWithEndpoints";

    public static final String QRY_FIND_BY_APPLICATIONID = "OpenApiDocument.findByApplicationId";

    public static final String QRY_FIND_BY_APPLICATIONID_WITH_ENDPOINTS = "OpenApiDocument.findByApplicationIdWithEndpoints";

    /**
     * The title of the service, corresponds to info.title of the Swagger document.
     */
    @Column(nullable = false)
    private String name;

    /**
     * This version refers to the info.version of the Swagger element.
     * It has nothing to do with the JPA entity versioning for optimistic locking.
     */
    @Column(nullable = false)
    private String version;

    @Column(length = 1024 * 1024)
    @Lob
    private String document;

    @OneToMany(targetEntity = Endpoint.class, mappedBy = "application", cascade = CascadeType.REMOVE)
    private Collection<Endpoint> endpoints = new ArrayList<>();

    private transient Swagger swagger;

    public OpenApiDocument() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getDocument() {
        return document;
    }

    public void setDocument(String document) {
        this.document = document;
    }

    public void setSwagger(Swagger swagger) {
        this.swagger = swagger;
    }

    public Swagger getSwagger() {
        return swagger;
    }

    public Collection<Endpoint> getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(Collection<Endpoint> endpoints) {
        this.endpoints = endpoints;
    }
}
