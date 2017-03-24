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

import com.intershop.gradle.versionrecommender.recommendation.RecommendationProviderContainer
import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

/**
 * The main extension of this plugin. It adds the DSL
 * for the configuration of version providers and
 * configuration for the update function.
 * It contains also some basic configuration for version
 * recommendation.
 */
@CompileStatic
class VersionRecommenderExtension {

    final static String EXTENSIONNAME = 'versionRecommendation'

    private Project project

    /**
     * Holds the version recommendation.
     */
    final RecommendationProviderContainer provider

    /**
     * Holds the update configuration.
     */
    final UpdateConfiguration updateConfiguration

    /**
     * Constructor with initialization of the basic configuration
     *
     * @param project The target project
     */
    VersionRecommenderExtension(Project project) {
        this.project = project
        provider = new RecommendationProviderContainer(project)

        updateConfiguration = new UpdateConfiguration(project)
        forceRecommenderVersion = false
    }

    /**
     * Creates the provider configuration from the
     * closure of the provider configuration.
     *
     * @param providerClosure
     */
    void provider(Closure providerClosure) {
        ConfigureUtil.configure(providerClosure, provider)
    }

    /**
     * Creates the update configuration from the
     * closure of the update configuration.
     *
     * @param updateConfigClosure
     */
    void updateConfiguration(final Closure updateConfigClosure) {
        project.configure(updateConfiguration, updateConfigClosure)
    }

    /**
     * If this variable true, the version is always taken from
     * the version recommendation. This will override configured
     * versions in dependencies.
     */
    boolean forceRecommenderVersion

    /**
     * This plugin applies the version recommendation to all
     * sub projects. Some times it is necessary to handle projects
     * different. Therefore it is possible to exclude projects by name.
     * With this configuration it is also possible to exclude the
     * root project.
     */
    List<String> excludeProjectsbyName = []
}
