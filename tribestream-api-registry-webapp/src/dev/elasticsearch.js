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

//
// Note 1: with tomee 7.0.2 we would write ~ this in the pom with jsCustomizers which will make it easier to reuse maven stuff
//

//
// Note 2: this impl should only use the JVM or main classes, not test one
//

// imports
var ClientBuilder = Java.type('javax.ws.rs.client.ClientBuilder');
var ProcessBuilder = Java.type('java.lang.ProcessBuilder');
var Thread = Java.type('java.lang.Thread');
var File = Java.type('java.io.File');
var FileWriter = Java.type('java.io.FileWriter');
var System = Java.type('java.lang.System');
var Files = Java.type('org.apache.openejb.loader.Files');
var runtime = Java.type('java.lang.Runtime').getRuntime();

// using local m2 to create the classpath, ensure it can be switch if not using the standard one
var m2 = props.getProperty("m2.dir", props.getProperty("user.home") + '/.m2/repository');
if (m2.charAt(m2.length - 1) != '/') {
    m2 = m2 + '/';
}

// create home folder
var home = Files.mkdirs(new File('target/dev/run/elasticsearch'));
if (home.exists()) {
    Files.delete(home);
}
var config = Files.mkdirs(new File(home, 'config'));
var loggingConfig = new FileWriter(new File(config, 'logging.yml'));
loggingConfig.write('es.logger.level: INFO\nrootLogger: INFO, console\nappender:\n  console:\n    \"type\": console\n    layout:\n      \"type\": consolePattern\n      conversionPattern: "[ELASTICSEARCH][%d{ISO8601}][%-5p][%-25c] %m%n"')
loggingConfig.close();

// boot it
var cp = [ // built dump "deps" in org.tomitribe.tribestream.registryng.test.elasticsearch.ElasticsearchServer, es version = 2.4.1
    m2 + 'com/carrotsearch/hppc/0.7.1/hppc-0.7.1.jar',
    m2 + 'com/fasterxml/jackson/core/jackson-core/2.8.1/jackson-core-2.8.1.jar',
    m2 + 'com/fasterxml/jackson/dataformat/jackson-dataformat-cbor/2.8.1/jackson-dataformat-cbor-2.8.1.jar',
    m2 + 'com/fasterxml/jackson/dataformat/jackson-dataformat-smile/2.8.1/jackson-dataformat-smile-2.8.1.jar',
    m2 + 'com/fasterxml/jackson/dataformat/jackson-dataformat-yaml/2.8.1/jackson-dataformat-yaml-2.8.1.jar',
    m2 + 'com/google/guava/guava/18.0/guava-18.0.jar',
    m2 + 'commons-cli/commons-cli/1.3.1/commons-cli-1.3.1.jar',
    m2 + 'com/ning/compress-lzf/1.0.2/compress-lzf-1.0.2.jar',
    m2 + 'com/spatial4j/spatial4j/0.5/spatial4j-0.5.jar',
    m2 + 'com/tdunning/t-digest/3.0/t-digest-3.0.jar',
    m2 + 'com/twitter/jsr166e/1.1.0/jsr166e-1.1.0.jar',
    m2 + 'io/netty/netty/3.10.6.Final/netty-3.10.6.Final.jar',
    m2 + 'joda-time/joda-time/2.9.4/joda-time-2.9.4.jar',
    m2 + 'log4j/log4j/1.2.17/log4j-1.2.17.jar',
    m2 + 'net/java/dev/jna/jna/4.1.0/jna-4.1.0.jar',
    m2 + 'org/apache/lucene/lucene-analyzers-common/5.5.2/lucene-analyzers-common-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-backward-codecs/5.5.2/lucene-backward-codecs-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-core/5.5.2/lucene-core-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-grouping/5.5.2/lucene-grouping-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-highlighter/5.5.2/lucene-highlighter-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-join/5.5.2/lucene-join-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-memory/5.5.2/lucene-memory-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-misc/5.5.2/lucene-misc-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-queries/5.5.2/lucene-queries-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-queryparser/5.5.2/lucene-queryparser-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-sandbox/5.5.2/lucene-sandbox-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-spatial3d/5.5.2/lucene-spatial3d-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-spatial/5.5.2/lucene-spatial-5.5.2.jar',
    m2 + 'org/apache/lucene/lucene-suggest/5.5.2/lucene-suggest-5.5.2.jar',
    m2 + 'org/elasticsearch/elasticsearch/2.4.1/elasticsearch-2.4.1.jar',
    m2 + 'org/elasticsearch/securesm/1.0/securesm-1.0.jar',
    m2 + 'org/hdrhistogram/HdrHistogram/2.1.6/HdrHistogram-2.1.6.jar',
    m2 + 'org/joda/joda-convert/1.2/joda-convert-1.2.jar',
    m2 + 'org/yaml/snakeyaml/1.15/snakeyaml-1.15.jar'
].join(props.getProperty('path.separator'));

// for debugging purposes: System.out.println('Elasticsearch classpath: ' + cp);

var process = new ProcessBuilder([
    props.getProperty('java.home') + '/bin/java',
    '-Des.security.manager.enabled=false',
    '-Des.nodes=localhost',
    '-Des.path.home=' + home.getAbsolutePath(),
    '-cp',
    cp,
    'org.elasticsearch.bootstrap.Elasticsearch',
    'start'
]).inheritIO().start();

// now wait we get http port opened
var retest = 120;
var client = ClientBuilder.newClient().property("http.connection.timeout", "5000");
try {
    while (retest > 0) {
        try {
            client.target('http://localhost:9200')
                .request()
                .get();
            break;
        } catch (e) {
            System.err.println('Elasticsearch not yet started, will retry in 1s (' + JSON.stringify(e) + ')');
            Thread.sleep(1000);
            retest--;
        }
    }
} finally {
    client.close();
}

// we are started, ensure we'll be stopped at shutdown
runtime.addShutdownHook(new Thread(function () {
    try {
        process.destroy();
        process.waitFor();
    } finally {
        Files.deleteOnExit(home);
    }
}));
