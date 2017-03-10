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

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.recommendation.RecommendationProvider
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
/**
 * <p>Set version for all configured providers if property exists and command is supported</p>
 * <p>The property name is composed of the provider name and 'Version'. </p>
 * <p>The main functionality is implemented in the connected providers.</p>
 */
@CompileStatic
class SetAllVersion  extends DefaultTask {

    /**
     * Provider list of this task
     */
    @Input
    List<RecommendationProvider> providers = []

    /**
     * Task action
     */
    @TaskAction
    void setVersion() {
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)

        providers.each { RecommendationProvider p ->
            if(ext.updateConfiguration.defaultUpdateProvider.contains(p.getName()) && p.getVersionFromProperty()) {
                p.setVersion()
                p.initializeVersion()
            }
        }
    }

    /**
     * Description
     *
     * @return "Reset all filter configurations"
     */
    @Internal
    @Override
    String getDescription() {
        return 'Set special version for all providers with configuration.'
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
