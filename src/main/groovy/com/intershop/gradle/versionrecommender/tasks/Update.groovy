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
package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.recommendation.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * <p>Update task of a project</p>
 * <p>This task updates version information of all providers
 * in the configured default list to the project configuration.
 * The main functionality is implemented in the connected providers.</P
 */
@CompileStatic
class Update extends DefaultTask {

    /**
     * Provider list of this task
     */
    @Input
    List<RecommendationProvider> providers = []

    /**
     * Task action
     */
    @TaskAction
    void runUpdate() {
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)

        ext.updateConfiguration.updateLogFile.delete()
        providers.each {RecommendationProvider p ->
            if(ext.updateConfiguration.defaultUpdateProvider.contains(p.getName())) {
                if(p.isVersionRequired() && ! (p.getVersionFromProperty())) {
                    throw new GradleException("It is necessary to specify a version property with -P${p.getVersionPropertyName()} = <version>.")
                }
                p.update(ext.updateConfiguration)
            }
        }
        println "See for more information in update log: ${ext.updateConfiguration.updateLogFile}"
    }

    /**
     * Description
     *
     * @return "Update Dependencies for configured default filters"
     */
    @Override
    String getDescription() {
        return "Update Dependencies for configured default filters"
    }

    /**
     * Group
     *
     * @return "Version Recommendation"
     */
    @Override
    String getGroup() {
        return "Version Recommendation"
    }
}
