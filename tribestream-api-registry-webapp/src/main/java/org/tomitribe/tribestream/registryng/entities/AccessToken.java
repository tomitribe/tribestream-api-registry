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

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;

@Entity
@Getter
@Setter
@NamedQueries({
        @NamedQuery(name = AccessToken.Queries.DELETE_EXPIRED_TOKENS, query = "DELETE AccessToken t WHERE t.expiryTimestamp < :now"),
        @NamedQuery(name = AccessToken.Queries.FIND_BY_TOKEN, query = "SELECT t from AccessToken t WHERE t.accessToken = :token")
})
public class AccessToken {
    // Are 8k sufficient for a token?
    private static final int MAX_ACCESS_TOKEN_LENGTH = 8192;

    private static final int MAX_SCOPE_LENGTH = 2048;

    public interface Queries {
        String DELETE_EXPIRED_TOKENS = "AccessToken.deleteExpiredTokens";
        String FIND_BY_TOKEN = "AccessToken.findByToken";
    }

    @Id
    @GeneratedValue
    private long id;

    @Column(length = MAX_ACCESS_TOKEN_LENGTH)
    private String accessToken;

    @Column(length = 512)
    private String username;

    @Column
    private long expiryTimestamp;

    @Column(length = MAX_SCOPE_LENGTH)
    private String scope;

}
