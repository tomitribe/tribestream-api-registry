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
import io.swagger.models.HttpMethod;
import io.swagger.models.Operation;
import io.swagger.models.Path;
import io.swagger.models.Swagger;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.tomitribe.tribestream.registryng.cdi.Tribe;
import org.tomitribe.tribestream.registryng.entities.Endpoint;
import org.tomitribe.tribestream.registryng.entities.HistoryEntry;
import org.tomitribe.tribestream.registryng.entities.OpenApiDocument;
import org.tomitribe.tribestream.registryng.security.LoginContext;
import org.tomitribe.tribestream.registryng.service.search.SearchEngine;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.PersistenceContext;
import javax.transaction.Transactional;
import java.io.StringWriter;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

/**
 * Central access to all OpenAPI documents.
 * Currently simply reads documents from the directory defined by the system property "openapirepo.dir".
 */
@Transactional
@ApplicationScoped
public class Repository {
    private static final Logger LOGGER = Logger.getLogger(Repository.class.getName());

    @PersistenceContext
    private EntityManager em;

    @Inject
    @Tribe
    private ObjectMapper mapper;

    @Inject
    private LoginContext loginContext;

    @Inject
    private SearchEngine searchEngine;

    public Endpoint findEndpointFromHumanReadableMeta(final String humanReadableApplicationName,
                                                      final String verb,
                                                      final String humanReadableEndpointPath,
                                                      final String version) {
        try {
            final List<Endpoint> endpoints =
                    ofNullable(version)
                            .filter(v -> !v.trim().isEmpty())
                            .map(v -> em.createNamedQuery(Endpoint.Queries.FIND_BY_HUMAN_REDABLE_PATH, Endpoint.class)
                                    .setParameter("applicationVersion", version))
                            .orElseGet(() -> em.createNamedQuery(Endpoint.Queries.FIND_BY_HUMAN_REDABLE_PATH_NO_VERSION, Endpoint.class))
                            .setParameter("applicationName", humanReadableApplicationName)
                            .setParameter("verb", verb)
                            .setParameter("endpointPath", humanReadableEndpointPath)
                            .setMaxResults(2)
                            .getResultList();
            if (endpoints.size() > 1) {
                throw new IllegalArgumentException("Conflicting endpoints: " + endpoints);
            }
            if (endpoints.isEmpty()) {
                return null;
            }
            return endpoints.get(0);
        } catch (final NoResultException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Could not find endpoint by {0}/{1}/{2}", new Object[]{humanReadableApplicationName, verb, humanReadableEndpointPath});
            }
            return null;
        }
    }

    public OpenApiDocument findApplicationFromHumanReadableMetadata(final String humanReadableApplicationName,
                                                                    final String version) {
        try {
            final List<OpenApiDocument> openApiDocuments =
                    ofNullable(version)
                            .filter(v -> !v.trim().isEmpty())
                            .map(v -> em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_HUMAN_REDABLE_NAME, OpenApiDocument.class)
                                    .setParameter("applicationVersion", version))
                            .orElseGet(() -> em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_HUMAN_REDABLE_NAME_NO_VERSION, OpenApiDocument.class))
                            .setParameter("applicationName", humanReadableApplicationName)
                            .setMaxResults(2)
                            .getResultList();
            if (openApiDocuments.size() > 1) {
                throw new IllegalArgumentException("Conflicting documents: " + openApiDocuments);
            }
            if (openApiDocuments.isEmpty()) {
                return null;
            }
            return openApiDocuments.get(0);
        } catch (final NoResultException e) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "Could not find endpoint by {0}", humanReadableApplicationName);
            }
            return null;
        }
    }

    public OpenApiDocument findByApplicationId(final String applicationId) throws NoResultException {
        try {
            return em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_APPLICATIONID, OpenApiDocument.class)
                    .setParameter("applicationId", applicationId)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "Could not find application by id {0}", applicationId);
            return null;
        }
    }

    public OpenApiDocument findByApplicationIdAndRevision(final String applicationid, final int revision) {
        AuditReader auditReader = AuditReaderFactory.get(em);
        OpenApiDocument openApiDocument = auditReader.find(OpenApiDocument.class, applicationid, revision);

        // Resolve the references here, because the Resource implementation will not be able to do that because
        // it is running another transaction
        for (Endpoint endpoint : openApiDocument.getEndpoints()) {
            endpoint.getDocument();
        }

        return openApiDocument;
    }

    public <T> List<HistoryEntry<T>> getRevisions(
            final Class<T> entityClass,
            final String id,
            final int first,
            final int pageSize) throws NoResultException {
        AuditReader auditReader = AuditReaderFactory.get(em);
        AuditQuery query = auditReader.createQuery().forRevisionsOfEntity(entityClass, false, true);
        query.add(
                AuditEntity.id().eq(id)
        );
        query.addOrder(
                AuditEntity.revisionNumber().desc()
        );
        query.setFirstResult(first).setMaxResults(pageSize);

        List<Object[]> objects = query.getResultList();
        return objects.stream().map(HistoryEntry<T>::new).collect(toList());
    }

    /**
     * Returns the number of revisions available for the entity with the given id.
     *
     * @param entityClass the Entity class type to use to find revisions
     * @param id          the ID of the entity
     * @return the number of revisions
     */
    public <T> int getNumberOfRevisions(final Class<T> entityClass, final String id) {
        final AuditQuery query = AuditReaderFactory.get(em).createQuery().forRevisionsOfEntity(entityClass, true, true);
        query.add(
                AuditEntity.id().eq(id)
        );
        query.addProjection(AuditEntity.revisionNumber().count());

        return ((Number) query.getSingleResult()).intValue();
    }


    public OpenApiDocument findByApplicationIdWithEndpoints(final String applicationId) {
        try {
            return em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_APPLICATIONID_WITH_ENDPOINTS, OpenApiDocument.class)
                    .setParameter("applicationId", applicationId)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "Could not find application by id {0}", applicationId);
            return null;
        }
    }

    public Endpoint findEndpointByIdAndRevision(final String endpointId, final int revision) {
        final AuditReader auditReader = AuditReaderFactory.get(em);
        final Endpoint endpoint = auditReader.find(Endpoint.class, endpointId, revision);

        // Resolve the application here, because the caller will not be able to, as it will
        // be running in a different transaction
        endpoint.getApplication().getId();

        return endpoint;
    }

    public OpenApiDocument findApplicationByNameAndVersion(final String name, final String version) {
        try {
            return em.createNamedQuery(OpenApiDocument.Queries.FIND_BY_NAME_AND_VERSION, OpenApiDocument.class)
                    .setParameter("name", name)
                    .setParameter("version", version)
                    .getSingleResult();
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "Could not find application by name '{0}' and version '{1}'", new Object[]{name, version});
            return null;
        }
    }

    public List<OpenApiDocument> findAllApplicationsMetadata() {
        List<OpenApiDocument> result = em.createNamedQuery(OpenApiDocument.Queries.FIND_ALL_METADATA, OpenApiDocument.class)
                .getResultList();

        return result;
    }

    public List<OpenApiDocument> findAllApplicationsWithEndpoints() {
        return em.createNamedQuery(OpenApiDocument.Queries.FIND_ALL_WITH_ENDPOINTS, OpenApiDocument.class)
                .getResultList();
    }

    public Endpoint findEndpointById(String endpointId) {

        try {
            return em.find(Endpoint.class, endpointId);
        } catch (NoResultException e) {
            LOGGER.log(Level.FINE, "Could not find endpoint by id %s", endpointId);
            // Not really nice, should be an Optional.
            // Forwarding the exception makes the caller only receive an indistinguishable EJBException
            return null;
        }
    }

    public Collection<Endpoint> findAllEndpoints() {
        return em.createNamedQuery(Endpoint.Queries.FIND_ALL_WITH_APPLICATION, Endpoint.class)
                .getResultList();
    }

    public OpenApiDocument insert(final Swagger swagger) {

        final OpenApiDocument document = new OpenApiDocument();
        document.setName(swagger.getInfo().getTitle());
        document.setVersion(swagger.getInfo().getVersion());

        final Swagger clone = createShallowCopy(swagger);
        clone.setPaths(null);
        document.setSwagger(clone);

        final String username = getUser();

        Date now = new Date();
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        document.setCreatedBy(username);
        document.setUpdatedBy(username);
        em.persist(document);

        // Store the endpoints in a separate table
        if (swagger.getPaths() != null) {
            for (Map.Entry<String, Path> stringPathEntry : swagger.getPaths().entrySet()) {
                final String path = stringPathEntry.getKey();
                final Path pathObject = stringPathEntry.getValue();
                for (Map.Entry<HttpMethod, Operation> httpMethodOperationEntry : pathObject.getOperationMap().entrySet()) {
                    final String verb = httpMethodOperationEntry.getKey().name().toUpperCase();
                    final Operation operation = httpMethodOperationEntry.getValue();

                    Endpoint endpoint = new Endpoint();
                    endpoint.setApplication(document);
                    endpoint.setPath(path);
                    endpoint.setVerb(verb);
                    endpoint.setOperation(operation);

                    em.persist(endpoint);
                    em.flush();
                    searchEngine.indexEndpoint(endpoint);
                }
            }
        }
        return document;
    }

    protected String getUser() {
        return loginContext.getUsername();
    }

    public Endpoint insert(final Endpoint endpoint, final String applicationId) {
        OpenApiDocument application = findByApplicationId(applicationId);
        application.getEndpoints().add(endpoint);

        endpoint.setApplication(application);
        Date now = new Date();
        endpoint.setCreatedAt(now);
        endpoint.setUpdatedAt(now);
        endpoint.setCreatedBy(getUser());
        endpoint.setUpdatedBy(getUser());

        application.setUpdatedAt(now);
        application.setUpdatedBy(getUser());
        em.persist(endpoint);
        update(application);

        em.flush();
        searchEngine.indexEndpoint(endpoint);

        return endpoint;
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

    public OpenApiDocument update(OpenApiDocument document) {
        document.setUpdatedAt(new Date());
        document.setUpdatedBy(loginContext.getUsername());
        if (document.getSwagger() != null) {
            document.setDocument(convertToJson(document.getSwagger()));
        }
        return em.merge(document);
    }

    public Endpoint update(Endpoint endpoint) {
        endpoint.setUpdatedAt(new Date());
        endpoint.setUpdatedBy(loginContext.getUsername());
        if (endpoint.getOperation() != null) {
            endpoint.setDocument(convertToJson(endpoint.getOperation()));
        }
        searchEngine.indexEndpoint(endpoint);
        return em.merge(endpoint);
    }

    private String convertToJson(Object object) {
        try (StringWriter sw = new StringWriter()) {
            mapper.writeValue(sw, object);
            sw.flush();
            return sw.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public boolean deleteApplication(final String applicationId) {
        final OpenApiDocument document = findByApplicationId(applicationId);
        if (document == null) {
            return false;
        } else {
            ofNullable(document.getEndpoints()).ifPresent(e -> e.forEach(searchEngine::deleteEndpoint));
            em.remove(document);
            return true;
        }
    }

    public boolean deleteEndpoint(String applicationId, String endpointId) {
        final Endpoint endpoint = findEndpointById(endpointId);
        if (endpoint == null || !applicationId.equals(endpoint.getApplication().getId())) {
            return false;
        } else {
            searchEngine.deleteEndpoint(endpoint);
            em.remove(endpoint);
            return true;
        }
    }
}
