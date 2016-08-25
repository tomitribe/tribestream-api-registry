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
package org.tomitribe.tribestream.registryng.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.service.serialization.SwaggerJsonMapperProducer;
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;

import javax.ejb.ConcurrencyManagement;
import javax.ejb.ConcurrencyManagementType;
import javax.ejb.Singleton;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Central access to all OpenAPI documents.
 * Currently simply reads documents from the directory defined by the system property "openapirepo.dir".
 */
@Singleton
@ConcurrencyManagement(ConcurrencyManagementType.BEAN)
public class Repository {

    @PersistenceContext
    private EntityManager em;

    @Inject
    @Named(SwaggerJsonMapperProducer.SWAGGER_OBJECT_MAPPER_NAME)
    private ObjectMapper mapper;

    public static String getApplicationId(Swagger swagger) {
        return swagger.getInfo().getTitle() + "-" + swagger.getInfo().getVersion();
    }

    public Collection<OpenApiDocument> getAllOpenApiDocuments() {
        return em.createNamedQuery(OpenApiDocument.QRY_FIND_ALL, OpenApiDocument.class).getResultList();
    }

    public Collection<Endpoint> getAllEndpoints() {
        List<Endpoint> result = em.createNamedQuery(Endpoint.QRY_FIND_ALL, Endpoint.class).getResultList();
        for (Endpoint endpoint : result) {
            // Eagerly resolve here
            endpoint.getApplication();
        }
        return result;
    }

    public OpenApiDocument findByApplicationId(String applicationId) throws NoResultException {
        try {
            return em.createNamedQuery(OpenApiDocument.QRY_FIND_BY_APPLICATIONID, OpenApiDocument.class)
                .setParameter("applicationId", applicationId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public OpenApiDocument findByApplicationIdWithEndpoints(String applicationId) {
        try {
            return em.createNamedQuery(OpenApiDocument.QRY_FIND_BY_APPLICATIONID_WITH_ENDPOINTS, OpenApiDocument.class)
                .setParameter("applicationId", applicationId)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public OpenApiDocument findApplicationByNameAndVersion(final String name, final String version) {
        try {
            return em.createNamedQuery(OpenApiDocument.QRY_FIND_BY_NAME_AND_VERSION, OpenApiDocument.class)
                    .setParameter("name", name)
                    .setParameter("version", version)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<OpenApiDocument> findAllApplications() {
        return em.createNamedQuery(OpenApiDocument.QRY_FIND_ALL, OpenApiDocument.class)
            .getResultList();
    }

    public List<OpenApiDocument> findAllApplicationsWithEndpoints() {
        return em.createNamedQuery(OpenApiDocument.QRY_FIND_ALL_WITH_ENDPOINTS, OpenApiDocument.class)
            .getResultList();
    }

    public Endpoint findEndpointById(String endpointId) {

        try {
            return em.find(Endpoint.class, endpointId);
        } catch (NoResultException e) {
            // Not really nice, should be an Optional.
            // Forwarding the exception makes the caller only receive an indistinguishable EJBException
            return null;
        }
    }

    public Endpoint findEndpoint(final String applicationId, final String verb, final String path) {
        try {
            return em.createNamedQuery(Endpoint.QRY_FIND_BY_APPLICATIONID_VERB_AND_PATH, Endpoint.class)
                .setParameter("applicationId", applicationId)
                .setParameter("verb", verb)
                .setParameter("path", path.startsWith("/") ? path : "/" + path)
                .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public Collection<Endpoint> findAllEndpoints() {
        return em.createNamedQuery(Endpoint.QRY_FIND_ALL_WITH_APPLICATION, Endpoint.class)
            .getResultList();
    }

    public OpenApiDocument insert(Swagger swagger) {

        final OpenApiDocument document = new OpenApiDocument();
        document.setName(swagger.getInfo().getTitle());
        document.setVersion(swagger.getInfo().getVersion());

        final Swagger clone = createShallowCopy(swagger);
        clone.setPaths(null);
        document.setSwagger(clone);

        em.persist(document);

        // Store the endpoints in a separate table
        for (Map.Entry<String, Path> stringPathEntry : swagger.getPaths().entrySet()) {
            final String path = stringPathEntry.getKey();
            final Path pathObject = stringPathEntry.getValue();
            for (Map.Entry<HttpMethod, Operation> httpMethodOperationEntry : pathObject.getOperationMap().entrySet()) {
                final String verb = httpMethodOperationEntry.getKey().name();
                final Operation operation = httpMethodOperationEntry.getValue();

                Endpoint endpoint = new Endpoint();
                endpoint.setApplication(document);
                endpoint.setPath(path);
                endpoint.setVerb(verb);
                endpoint.setOperation(operation);

                em.persist(endpoint);
            }
        }

        return document;
    }

    public static Swagger createShallowCopy(Swagger swagger) {
        Swagger result = new Swagger();
        result.setSwagger(swagger.getSwagger());
        result.info(swagger.getInfo());
        result.setHost(swagger.getHost());
        result.setBasePath(swagger.getBasePath());
        result.setSchemes(swagger.getSchemes());
        result.setConsumes(swagger.getConsumes());
        result.setProduces(swagger.getProduces());
        result.setPaths(swagger.getPaths());
        result.setDefinitions(swagger.getDefinitions());
        result.setParameters(swagger.getParameters());
        result.setResponses(swagger.getResponses());
        result.setSecurityDefinitions(swagger.getSecurityDefinitions());
        result.setSecurity(swagger.getSecurity());
        result.setTags(swagger.getTags());
        result.setExternalDocs(swagger.getExternalDocs());
        return result;
    }
}
