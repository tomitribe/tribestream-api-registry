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

import org.apache.lucene.store.IndexOutput;

import javax.enterprise.inject.Vetoed;
import java.io.IOException;

@Vetoed // note that atomicity is to rework a bit
public class JPAIndexOutput extends IndexOutput {
    private final JPADirectoryFactory.JPADirectory directory;

    public JPAIndexOutput(final JPADirectoryFactory.JPADirectory directory, final String name) {
        super("JPAIndexOutput(marker=" + directory.getMarker() + ", name=" + name + ")", name);
        this.directory = directory;
    }

    @Override
    public void close() throws IOException {
        // no-op
    }

    @Override
    public long getFilePointer() {
        return directory.getFilePointer(getName());
    }

    @Override
    public long getChecksum() throws IOException {
        return directory.getChecksum(getName());
    }

    @Override
    public void writeByte(final byte b) throws IOException {
        directory.writeBytes(getName(), new byte[]{b}, 0, 1);
    }

    @Override
    public void writeBytes(final byte[] b, final int offset, final int length) throws IOException {
        directory.writeBytes(getName(), b, offset, length);
    }
}
