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
package org.tomitribe.tribestream.registryng.lucene;

import javax.enterprise.inject.Vetoed;
import javax.persistence.Column;
import javax.persistence.Embeddable;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.Version;
import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.util.Objects;

import static java.util.Optional.ofNullable;

@Entity
@Table(name = "lucene_index")
@NamedQueries({
        @NamedQuery(name = "File.findAll", query = "select i from File i")
})
@Vetoed
public class File {
    @EmbeddedId
    private IndexId id;

    private long fileLength;
    private int filePointer;

    @Lob
    @Column(length = 491520 /*64k*/)
    private byte[] content;

    @Version
    private long revision;

    @Transient
    private ByteArrayOutputStream inMemoryBuffer;

    @PreUpdate
    @PrePersist
    public void flush() {
        if (inMemoryBuffer != null) { // just for perf
            final byte[] appended = inMemoryBuffer.toByteArray();
            final int newLength = ofNullable(content).map(c -> c.length).orElse(0) + appended.length;
            final byte[] newInstance = new byte[newLength];
            if (content != null && content.length > 0) {
                System.arraycopy(content, 0, newInstance, 0, content.length);
                System.arraycopy(appended, 0, newInstance, content.length, appended.length);
                content = newInstance;
            } else {
                content = appended;
            }
            inMemoryBuffer = null;
        }
    }

    public IndexId getId() {
        return id;
    }

    public void setId(final IndexId id) {
        this.id = id;
    }

    public long getFileLength() {
        return fileLength;
    }

    public void setFileLength(final long fileLength) {
        this.fileLength = fileLength;
    }

    public byte[] getContent() {
        flush();
        return content;
    }

    public void setContent(final byte[] content) {
        this.content = content;
    }

    public int getFilePointer() {
        return filePointer + ofNullable(inMemoryBuffer).map(ByteArrayOutputStream::size).orElse(0);
    }

    public void setFilePointer(final int filePointer) {
        this.filePointer = filePointer;
    }

    public long getRevision() {
        return revision;
    }

    public void setRevision(final long revision) {
        this.revision = revision;
    }

    public void write(final int offset, final int length, final byte... bytes) {
        if (inMemoryBuffer == null) {
            inMemoryBuffer = new ByteArrayOutputStream();
        }
        inMemoryBuffer.write(bytes, offset, length);
        fileLength += length;
        filePointer += length;
    }

    @Vetoed
    @Embeddable
    public static class IndexId implements Serializable {
        private String marker;

        @Column(length = 2048)
        private String name;

        public IndexId() {
            // no-op
        }

        public IndexId(final String marker, final String name) {
            this.marker = marker;
            this.name = name;
        }

        public String getMarker() {
            return marker;
        }

        public void setMarker(final String marker) {
            this.marker = marker;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || !IndexId.class.isInstance(o)) {
                return false;
            }
            final IndexId indexId = IndexId.class.cast(o);
            return marker.equals(indexId.marker) && name.equals(indexId.name);

        }

        @Override
        public int hashCode() {
            return Objects.hash(marker, name);
        }
    }
}
