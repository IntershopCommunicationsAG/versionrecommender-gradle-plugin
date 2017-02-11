package com.intershop.gradle.versionrecommender.provider

import groovy.util.logging.Slf4j
import org.gradle.api.Project

@Slf4j
class IvyProvider extends AbstractFileBasedProvider {

    IvyProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
    }

    @Override
    String getShortTypeName() {
        return 'ivy'
    }

    @Override
    void fillVersionMap() {
        InputStream stream = getStream()
        if(stream) {
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
