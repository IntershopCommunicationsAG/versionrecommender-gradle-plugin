/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.util.FileInputType
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
    synchronized void fillVersionMap() {
        InputStream stream = getStream()
        if(stream) {
            log.info('Prepare version list from {} of {}.', getShortTypeName(), getName())

            def ivyconf = new XmlSlurper().parse(stream)
            ivyconf.dependencies.dependency.each {
                String descr = "${it.@org.text()}:${it.@name.text()}".toString()
                String version = "${it.@rev.text()}"
                versions.put(descr, version)
                if (transitive) {
                    calculateDependencies(descr, version)
                }
            }
            if(inputType == FileInputType.DEPENDENCYMAP && inputDependency.get('version')) {
                versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), inputDependency.get('version').toString())
            }
        } else {
            project.logger.info('It is not possible to identify versions for {}. Please check your configuration.', getName())
        }
    }
}
