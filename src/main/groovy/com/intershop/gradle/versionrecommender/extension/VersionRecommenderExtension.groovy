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
package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

@CompileStatic
class VersionRecommenderExtension {

    final static String EXTENSIONNAME = 'versionRecommendation'

    private Project project

    final RecommendationProviderContainer provider

    final String[] defaultUpdateConfigurations

    final UpdateConfiguration updateConfiguration

    VersionRecommenderExtension(Project project) {
        this.project = project
        provider = new RecommendationProviderContainer(project)

        updateConfiguration = new UpdateConfiguration(project)
        defaultUpdateConfigurations = []
    }

    void updateConfiguration(final Closure c) {
        project.configure(updateConfiguration, c)
    }

    void provider(Closure c) {
        ConfigureUtil.configure(c, provider)
    }

    boolean forceRecommenderVersion = false
}
