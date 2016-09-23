package org.tomitribe.tribestream.registryng.hibernate;

import org.hibernate.jpa.boot.scan.spi.ScanOptions;
import org.hibernate.jpa.boot.scan.spi.ScanResult;
import org.hibernate.jpa.boot.scan.spi.Scanner;
import org.hibernate.jpa.boot.spi.ClassDescriptor;
import org.hibernate.jpa.boot.spi.MappingFileDescriptor;
import org.hibernate.jpa.boot.spi.PackageDescriptor;
import org.hibernate.jpa.boot.spi.PersistenceUnitDescriptor;

import java.util.Set;

import static java.util.Collections.emptySet;

public class NoScan implements Scanner {
    @Override
    public ScanResult scan(final PersistenceUnitDescriptor persistenceUnit, final ScanOptions options) {
        return new ScanResult() {
            @Override
            public Set<PackageDescriptor> getLocatedPackages() {
                return emptySet();
            }

            @Override
            public Set<ClassDescriptor> getLocatedClasses() {
                return emptySet();
            }

            @Override
            public Set<MappingFileDescriptor> getLocatedMappingFiles() {
                return emptySet();
            }
        };
    }
}
