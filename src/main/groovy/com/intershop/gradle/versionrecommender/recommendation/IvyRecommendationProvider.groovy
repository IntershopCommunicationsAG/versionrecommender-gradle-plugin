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
package com.intershop.gradle.versionrecommender.recommendation

import com.intershop.gradle.versionrecommender.util.FileInputType
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * This class implements the access to an Ivy descriptor.
 */
@CompileStatic
@Slf4j
class IvyRecommendationProvider extends FileBasedRecommendationProvider {

    /**
     * Constructor is called by configur(Closure)
     *
     * @param name      the name of the provider
     * @param project   the target project
     */
    IvyRecommendationProvider(final String name, final Project project)  {
        super(name, project)
    }

    /**
     * Addditonal constructor with a parameter for the input.
     *
     * @param name      the name of the provider
     * @param project   the target project
     * @param input     input object, can be an File, URL, String or dependency map
     */
    IvyRecommendationProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
    }

    /**
     * Returns a type name of an special implementation.
     *
     * @return returns always ivy
     */
    @Override
    String getShortTypeName() {
        return 'ivy'
    }

    /**
     * Map with all version information of the provider will
     * be calculated by this method. Before something happens
     * versions is checked for 'null'.
     * The key is a combination of the group or organisation
     * and the name or artifact id. The value is the version.
     */
    @Override
    void fillVersionMap() {
        InputStream stream = getStream()
        if(stream) {
            log.info('Prepare version list from {} of {}.', getShortTypeName(), getName())

            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance()
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder()
            Document ivy = dBuilder.parse(stream)
            ivy.getDocumentElement().normalize()

            NodeList depsNL = ivy.getElementsByTagName('dependencies')
            NodeList depNL = depsNL.length > 0 ? ((Element)depsNL.item(0)).getElementsByTagName('dependency'): null
            if(depNL && depNL.length > 0) {
                for (int temp = 0; temp < depNL.getLength(); temp++) {
                    Element dep = (Element) depNL.item(temp)

                    String descr = "${dep.getAttribute('org')}:${dep.getAttribute('name')}".toString()
                    String version = dep.getAttribute('rev')
                    versions.put(descr, version)
                    if (transitive) {
                        calculateDependencies(descr, version)
                    }
                }
                if (inputType == FileInputType.DEPENDENCYMAP) {
                    versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), super.getVersionFromConfig())
                }

                log.info('Prepare version list from {} of {} - finished.', getShortTypeName(), getName())
            } else {
                log.info('Ivy file for {} does not contain dependencies.', getShortTypeName())
            }
        } else {
            project.logger.info('It is not possible to identify versions for {}. Please check your configuration.', getName())
        }
    }
}
