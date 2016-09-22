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
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.tomitribe.tribestream.registryng.entities;

import org.hibernate.envers.RevisionType;

import java.util.Date;

/**
 * This class is used to transport historic entities from Envers to the Repository.
 * Envers only returns an Object[] containing the historic entity, the revision and the revision type.
 * This class is easier to consume.
 * @param <T>
 */
public class HistoryEntry<T> {

    private final T historicObject;

    private final Revision revision;

    private final RevisionType revisionType;

    /**
     * Creates a new HistoryEntry from the results returned by the AuditReader.
     * That means the given Object array must contain the historic entity, the associated revision entity and the
     * revision type (Create, Update, Delete)
     * @param auditQueryResult
     */
    public HistoryEntry(final Object[] auditQueryResult) {
        historicObject = (T) auditQueryResult[0];
        revision = (Revision) auditQueryResult[1];
        revisionType = (RevisionType) auditQueryResult[2];
    }

    public HistoryEntry(final T historicObject, final Revision revision, final RevisionType revisionType) {
        this.historicObject = historicObject;
        this.revision = revision;
        this.revisionType = revisionType;
    }

    public T getHistoricObject() {
        return historicObject;
    }

    public Revision getRevision() {
        return revision;
    }

    public RevisionType getRevisionType() {
        return revisionType;
    }

    @Override
    public String toString() {
        return String.format("HistoryEntry[object=%s, revType=%s, revId=%d, timestamp=%s, user=%s]",
                historicObject,
                revisionType,
                revision.getId(),
                new Date(revision.getTimestamp()),
                revision.getUsername());
    }
}
