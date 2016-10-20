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
package org.tomitribe.tribestream.registryng.test.elasticsearch;

import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.Thread.sleep;
import static java.util.Arrays.asList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.joining;
import static org.jboss.shrinkwrap.resolver.api.maven.Maven.configureResolver;

public class ElasticsearchServer implements Closeable {
    private static final int POLLING_TIMEOUT = 500;

    private volatile Process process;
    private volatile Thread shutdownHook;
    private volatile File home;

    private final String workDir;
    private final String version;
    private int httpPort;
    private int tcpPort;

    public ElasticsearchServer(final String workDir, final String version) {
        this.workDir = workDir;
        this.version = version;
    }

    public ElasticsearchServer start() {
        final int tcpPort;
        final int httpPort;
        try (final ServerSocket rdm = new ServerSocket(0)) {
            httpPort = rdm.getLocalPort();

            try (final ServerSocket rdm2 = new ServerSocket(0)) { // while other port is hold otherwise we could get the same
                tcpPort = rdm2.getLocalPort();
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        return start(httpPort, tcpPort);
    }
    public ElasticsearchServer start(final int httpPort, final int tcpPort) {

        this.httpPort = httpPort;
        this.tcpPort = tcpPort;

        home = new File(ofNullable(workDir).orElseGet(() -> "target/elasticsearch-" + System.nanoTime()));
        final File config = new File(home, "config");
        home.mkdirs();
        config.mkdirs();
        try (final Writer w = new FileWriter(new File(config, "logging.yml"))) {
            w.write(
                    "es.logger.level: INFO\n" +
                            "rootLogger: INFO, console\n" +
                            "appender:\n" +
                            "  console:\n" +
                            "    \"type\": console\n" +
                            "    layout:\n" +
                            "      \"type\": consolePattern\n" +
                            "      conversionPattern: \"[ELASTICSEARCH][%d{ISO8601}][%-5p][%-25c] %m%n\"");
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }

        final ConfigurableMavenResolverSystem mvn = configureResolver();

        final Collection<File> deps = new ArrayList<>();
        deps.addAll(new ArrayList<>(asList(mvn.resolve("org.elasticsearch:elasticsearch:" + version).withTransitivity().asFile())));
        deps.addAll(Stream.of("net.java.dev.jna:jna:4.1.0", "log4j:log4j:1.2.17")
                .map(d -> mvn.resolve(d).withoutTransitivity().asSingleFile())
                .collect(Collectors.toList()));
        try {
            final String[] command = buildCommand(httpPort, tcpPort, home, deps);
            process = new ProcessBuilder(command).inheritIO().start();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        shutdownHook = new Thread() {
            @Override
            public void run() {
                ElasticsearchServer.this.close();
            }
        };
        Runtime.getRuntime().addShutdownHook(shutdownHook);

        waitForHttp(httpPort, TimeUnit.MINUTES.toMillis(2));

        return this;
    }

    public int getPort() {
        return httpPort;
    }

    @Override
    public void close() {
        if (process != null) {
            process.destroy();
            try {
                process.exitValue();
            } catch (final IllegalThreadStateException itse) {
                process.destroyForcibly();
            }
        }
        try {
            Runtime.getRuntime().removeShutdownHook(shutdownHook);
        } catch (final IllegalStateException ise) {
            // ok we are already shutting down
        }
        process = null;
        shutdownHook = null;

        System.gc(); // mainly for windows and to try to enforce files to be released before deleting them
        try {
            Files.walkFileTree(home.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs) throws IOException {
                    Files.delete(file);
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException exc) throws IOException {
                    Files.delete(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (final IOException e) {
            if (home.exists()) {
                throw new IllegalStateException(e);
            } // else we are fine, just a shutdown hooks ordering issue, not a big deal
        }
    }


    private void waitForHttp(final int port, final long startTimeout) {
        final long start = System.currentTimeMillis();
        while (startTimeout <= 0 || System.currentTimeMillis() - start < startTimeout) {
            try (final Socket ignored = new Socket("localhost", port)) {
                return;
            } catch (final UnknownHostException e) {
                throw new IllegalArgumentException(e);
            } catch (final IOException e) {
                // try again
            }

            try {
                sleep(POLLING_TIMEOUT);
            } catch (final InterruptedException e) {
                Thread.interrupted();
                throw new IllegalStateException(e);
            }
        }
    }

    private static String[] buildCommand(final int port, final int tcpPort, final File home, final Collection<File> classpath) {
        final Collection<String> cmd = new ArrayList<>();
        cmd.add(new File(System.getProperty("java.home"), "bin/java").getAbsolutePath());
        if (Boolean.getBoolean("elasticsearch.debug")) {
            cmd.add("-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5005");
        }
        cmd.add("-Des.security.manager.enabled=false");
        cmd.add("-Des.path.home=" + home.getAbsolutePath());
        cmd.add("-Des.http.port=" + port);
        cmd.add("-Des.transport.tcp.port=" + tcpPort);
        cmd.add("-Des.nodes=localhost");
        cmd.add("-cp");
        cmd.add(classpath.stream().map(File::getPath).collect(joining(System.getProperty("path.separator"))));
        cmd.add("org.elasticsearch.bootstrap.Elasticsearch");
        cmd.add("start");
        return cmd.toArray(new String[cmd.size()]);
    }
}
