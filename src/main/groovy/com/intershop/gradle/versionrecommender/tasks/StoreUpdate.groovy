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
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * <p>Store update task of a project</p>
 * <p>This task stores all updated version information of all providers
 * in the configured default list to the project configuration.
 * The main functionality is implemented in the connected providers.</P
 */
@CompileStatic
class StoreUpdate extends DefaultTask {

    /**
     * Provider list of this task
     */
    @Input
    List<RecommendationProvider> providers = []

    /**
     * Input map for version files
     */
    @Input
    Map<String,File> versionFiles = [:]

    /**
     * Task action
     */
    @TaskAction
    void storeChanges() {
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)

        providers.each {RecommendationProvider p ->
            if(ext.updateConfiguration.defaultUpdateProvider.contains(p.getName())) {
                try {
                    File vf = p.store(versionFiles.get(p.getName()))
                }catch (IOException ex) {
                    throw new GradleException("It was not possible to store changes of '${p.getName()}' (${ex.getMessage()})!")
                }
            }
        }
    }

    /**
     * Description
     *
     * @return "Store changes from working dir to configuration"
     */
    @Internal
    @Override
    String getDescription() {
        return "Store changes from working dir to configuration"
    }

    /**
     * Group
     *
     * @return "Version Recommendation"
     */
    @Internal
    @Override
    String getGroup() {
        return "Version Recommendation"
    }
}
