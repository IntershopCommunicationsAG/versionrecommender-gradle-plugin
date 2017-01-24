package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.util.FileInputType
import groovy.util.logging.Slf4j
import org.gradle.api.Project

@Slf4j
class IvyProvider extends AbstractFileBasedProvider {

    IvyProvider(final String name, final Project project, final File inputFile) {
        super(name, project, inputFile)
    }

    IvyProvider(final String name, final Project project, final Object dependencyNotation) {
        super(name, project, dependencyNotation)
    }

    IvyProvider(final String name, final Project project, final URL inputURL) {
        super(name, project, inputURL)
    }

    IvyProvider(final String name, final Project project, final URI inputURI) {
        super(name, project, inputURI)
    }

    IvyProvider(final String name, final Project project, final String input, final FileInputType type) {
        super(name, project, input, type)
    }

    @Override
    String getShortTypeName() {
        return 'ivy'
    }

    @Override
    void fillVersionMap() {
        if(versions == null) {
            InputStream stream = getStream()
            if(stream) {
                versions = [:]
                def ivyconf = new XmlSlurper().parse(stream)

                ivyconf.dependencies.dependency.each {
                    String descr = "${it.@org.text()}:${it.@name.text()}".toString()
                    String version = "${it.@rev.text()}"
                    versions.put(descr, version)
                    if (transitive) {
                        calculateDependencies(descr, version)
                    }
                }
            }
        }
    }
}
