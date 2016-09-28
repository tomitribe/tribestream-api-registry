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

import org.apache.lucene.index.IndexFileNames;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.IOContext;
import org.apache.lucene.store.IndexInput;
import org.apache.lucene.store.IndexOutput;
import org.apache.lucene.store.Lock;
import org.apache.lucene.store.NoLockFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.ejb.ConcurrencyManagement;
import javax.ejb.SessionContext;
import javax.ejb.Singleton;
import javax.ejb.TransactionAttribute;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.zip.CRC32;

import static java.util.Optional.ofNullable;
import static javax.ejb.ConcurrencyManagementType.BEAN;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

// note that operation are not in memory for now (they don't use close() as flush), if perf are an issue let's do it
@Singleton
@ConcurrencyManagement(BEAN)
public class JPADirectoryFactory {
    @PersistenceContext
    private EntityManager em;

    @Resource
    private SessionContext ctx;

    private JPADirectoryFactory self;

    @PostConstruct
    private void captureFacade() {
        self = ctx.getBusinessObject(JPADirectoryFactory.class);
    }

    public Directory newInstance(final String marker) {
        return new JPADirectory(task -> self.execute(task), em, marker);
    }

    @TransactionAttribute(REQUIRES_NEW)
    public <T> T execute(final Supplier<T> task) {
        return task.get();
    }

    public static class JPADirectory extends Directory {
        private final Function<Supplier<?>, ?> txProvider;
        private final EntityManager em;
        private final String marker;

        JPADirectory(final Function<Supplier<?>, ?> txProvider, final EntityManager em, final String marker) {
            this.txProvider = txProvider;
            this.em = em;
            this.marker = marker;
        }

        @Override
        public String[] listAll() throws IOException {
            return em.createNamedQuery("File.findAll", File.class)
                    .getResultList().stream()
                    .filter(f -> f.getId().getMarker().equals(marker))
                    .map(e -> {
                        em.detach(e);
                        return e;
                    })
                    .map(f -> f.getId().getName())
                    .sorted()
                    .toArray(String[]::new);
        }

        @Override
        public void deleteFile(final String s) throws IOException {
            txProvider.apply(() -> {
                final File file = em.find(File.class, id(s));
                if (file != null) {
                    em.remove(file);
                }
                return null;
            });
        }

        @Override
        public long fileLength(final String s) throws IOException {
            return ofNullable(em.find(File.class, id(s))).map(File::getFileLength).orElse(-1L);
        }

        @Override
        public IndexOutput createOutput(final String s, final IOContext ioContext) throws IOException {
            deleteFile(s);
            return new JPAIndexOutput(this, s);
        }

        @Override
        public IndexOutput createTempOutput(final String prefix, final String suffix, final IOContext context) throws IOException {
            while (true) {
                final String name = IndexFileNames.segmentFileName(prefix, suffix + "_" + Long.toString(System.currentTimeMillis(), Character.MAX_RADIX), "tmp");
                if (em.find(File.class, id(name)) == null) {
                    return createOutput(name, context);
                }
            }
        }

        @Override
        public void sync(final Collection<String> collection) throws IOException {
            // no-op
        }

        @Override
        public void rename(final String source, final String dest) throws IOException {
            txProvider.apply(() -> {
                final File file = em.find(File.class, id(source));
                if (file != null) {
                    final File copy = new File();
                    copy.setId(id(dest));
                    copy.setFileLength(file.getFileLength());
                    copy.setFilePointer(file.getFilePointer());
                    copy.setContent(file.getContent());
                    em.remove(file);
                    em.persist(copy);
                }
                return null;
            });
        }

        @Override
        public void syncMetaData() throws IOException {
            // no-op
        }

        @Override
        public IndexInput openInput(final String s, final IOContext ioContext) throws IOException {
            return IndexInput.class.cast(ofNullable(txProvider.apply(() ->
                    ofNullable(em.find(File.class, id(s)))
                            .map(file -> new JPAIndexInput("JPAIndexInput(marker=" + marker + ", name=" + s + ")", file.getContent())).orElse(null)))
                    .orElseThrow(() -> new FileNotFoundException(s)));
        }

        @Override
        public Lock obtainLock(final String name) throws IOException {
            return NoLockFactory.INSTANCE.obtainLock(null, null);
        }

        @Override
        public void close() throws IOException {
            // no-op
        }

        public long getLength(final String name) {
            return Long.class.cast(txProvider.apply(() -> getDetachedFile(name).getFileLength()));
        }

        public int getFilePointer(final String name) {
            return Integer.class.cast(txProvider.apply(() -> getOrCreateFile(name).getFilePointer()));
        }

        public long getChecksum(final String name) {
            final CRC32 crc32 = new CRC32();
            crc32.update(byte[].class.cast(txProvider.apply(() -> getOrCreateFile(name).getContent())));
            return crc32.getValue();
        }

        public void writeBytes(final String name, final byte[] b, final int offset, final int length) {
            txProvider.apply(() -> {
                final File file = getOrCreateFile(name);
                file.write(offset, length, b);
                return null;
            });
        }

        private File getDetachedFile(final String name) {
            final File file = em.find(File.class, id(name));
            if (file != null) {
                em.detach(file);
            }
            return file;
        }

        private File getOrCreateFile(final String name) {
            final File.IndexId id = id(name);
            return ofNullable(em.find(File.class, id))
                    .orElseGet(() -> {
                        final File file = new File();
                        file.setId(id);
                        file.setFileLength(0);
                        file.setFilePointer(0);
                        file.setContent(new byte[0]);
                        em.persist(file);
                        return file;
                    });
        }

        private File.IndexId id(String s) {
            return new File.IndexId(marker, s);
        }

        String getMarker() {
            return marker;
        }
    }
}
