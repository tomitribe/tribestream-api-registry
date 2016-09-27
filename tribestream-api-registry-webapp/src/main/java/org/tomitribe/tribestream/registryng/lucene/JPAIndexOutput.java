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
