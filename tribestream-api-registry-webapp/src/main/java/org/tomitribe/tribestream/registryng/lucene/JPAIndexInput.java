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

import org.apache.lucene.store.IndexInput;

import javax.enterprise.inject.Vetoed;
import java.io.EOFException;
import java.io.IOException;

@Vetoed
public class JPAIndexInput extends IndexInput {
    private final byte[] content;
    private int pos = 0;

    public JPAIndexInput(final String description, final byte[] content) {
        super(description);
        this.content = content;
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    @Override
    public long getFilePointer() {
        return pos;
    }

    @Override
    public void seek(final long pos) throws IOException {
        if (pos >= length()) {
            throw new EOFException("pos=" + pos + ", length=" + length() + " ; can't move to that position");
        }
        this.pos = (int) pos;
    }

    @Override
    public long length() {
        return content.length;
    }

    @Override
    public IndexInput slice(final String sliceDescription, final long offset, final long length) throws IOException {
        if (offset < 0 || length < 0 || offset + length > length()) {
            throw new IllegalArgumentException("slice() " + sliceDescription + " out of bounds: " + this);
        }
        final byte[] sliceContent = new byte[(int) length];
        System.arraycopy(content, (int) offset, sliceContent, 0, (int) length);
        return new JPAIndexInput(getFullSliceDescription(sliceDescription), sliceContent) {{
            seek(0L);
        }};
    }

    @Override
    public byte readByte() throws IOException {
        final byte[] b = new byte[1];
        readBytes(b, 0, 1);
        return b[0];
    }

    @Override
    public void readBytes(final byte[] b, final int offset, final int len) throws IOException {
        final int available = (int) (length() - pos);
        final int bytesToCopy = len < available ? len : available;
        System.arraycopy(content, pos, b, offset, bytesToCopy);
        pos += bytesToCopy;
    }
}
