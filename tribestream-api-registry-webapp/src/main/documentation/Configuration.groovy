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
import org.apache.xbean.finder.AnnotationFinder
import org.apache.xbean.finder.archive.FileArchive

import static java.util.Locale.ENGLISH

class Config {
    def name
    def defaultValue
    def description
}
class Node {
    def shortName
    def level
    def children = [:]
    def childrenLeaves = []

    def dumpNode(writer) {
        (1..level).each { writer.write '=' }
        writer.writeLine "= ${shortName.capitalize()}"
        writer.writeLine ''

        if (!childrenLeaves.isEmpty()) {
            writer.writeLine '|==='
            writer.writeLine '|Name|Default|Description'
            childrenLeaves.sort { it.name }.each {
                writer.writeLine "|${it.name}|${it.defaultValue}|${it.description}"
            }
            writer.writeLine '|==='
            writer.writeLine ''
        }

        children.values()
                .sort { it.shortName.toLowerCase(ENGLISH) }
                .each { it.dumpNode(writer) }
    }

}

// retrieves all @ConfigProperty on fields and generate doc from it
def generateConfigurationDoc() {
    def classes = new File(project.build.outputDirectory).toURI().toURL()

    def urls = []
    urls.add(classes)
    project.artifacts.each { urls.add(it.file.toURI().toURL()) }

    log.debug("using classloader ${urls}")

    def configLoader = new URLClassLoader(urls as URL[])
    def markerConfig = configLoader.loadClass('org.apache.deltaspike.core.api.config.ConfigProperty')
    def markerDescription = configLoader.loadClass('org.tomitribe.tribestream.registryng.documentation.Description')
    def finder = new AnnotationFinder(new FileArchive(configLoader, classes), true)

    def tree = new Node(shortName: "${project.name} :: Configuration", level: 0)
    finder.findAnnotatedFields(markerConfig).each {
        def config = it.getAnnotation(markerConfig)
        def description = it.getAnnotation(markerDescription)
        if (description == null) {
            throw new IllegalStateException("No @Description for " + it)
        }

        def name = config.name()
        def current = tree;
        def values = name.replace('tribe.registry', '').tokenize('\\.')
        for (int i = 0; i < values.size() - 1; i++) {
            def key = values.get(i);
            def newNode = current.children[key]
            if (newNode == null) {
                newNode = new Node(shortName: key, level: current.level + 1)
                current.children[key] = newNode
            }
            current = newNode
        }
        current.childrenLeaves << new Config(
                name: name,
                defaultValue: config.defaultValue().replace('org.apache.deltaspike.NullValueMarker', "-"),
                description: description.value())
    }

    if (tree.children.isEmpty()) {
        throw new IllegalStateException("Something went bad, we didn't find any doc");
    }

    def configuration = new File(project.build.directory, "doc/src/configuration.adoc")
    configuration.parentFile.mkdirs()
    configuration.withWriter('utf-8') { w ->
        w.writeLine "= ${project.name} :: Configuration"
        w.writeLine ''
        tree.dumpNode(w)
    }

    container.lookup('org.apache.maven.project.MavenProjectHelper')
            .attachArtifact(project, 'adoc', 'configuration', configuration)
}

generateConfigurationDoc()
