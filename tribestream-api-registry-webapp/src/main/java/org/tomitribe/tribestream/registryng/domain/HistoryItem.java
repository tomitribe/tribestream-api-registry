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
package org.tomitribe.tribestream.registryng.domain;

import org.tomitribe.tribestream.registryng.entities.HistoryEntry;

/**
 * A list of these objects is sent to the client if it asks for the history of a certain application or endpoint.
 */
public class HistoryItem {

    private int revisionId;

    private long timestamp;

    private String username;

    private String usercomment;

    private String revisionType;

    public HistoryItem() {
    }

    public HistoryItem(HistoryEntry entry) {
        this.revisionId   = entry.getRevision().getId();
        this.timestamp    = entry.getRevision().getTimestamp();
        this.username     = entry.getRevision().getUsername();
        this.usercomment  = entry.getRevision().getUserComment();
        this.revisionType = entry.getRevisionType().name();
    }

    public int getRevisionId() {
        return revisionId;
    }

    public void setRevisionId(int revisionId) {
        this.revisionId = revisionId;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUsercomment() {
        return usercomment;
    }

    public void setUsercomment(String usercomment) {
        this.usercomment = usercomment;
    }

    public String getRevisionType() {
        return revisionType;
    }

    public void setRevisionType(String revisionType) {
        this.revisionType = revisionType;
    }

    @Override
    public String toString() {
        return "HistoryItem{" +
                "revisionId=" + revisionId +
                ", timestamp=" + timestamp +
                ", username='" + username + '\'' +
                ", usercomment='" + usercomment + '\'' +
                ", revisionType='" + revisionType + '\'' +
                '}';
    }
}
