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
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

/**
 * <p>Extend version with LOCAL</p>
 * <p>The version configuration of the provider will be extended with
 * the extension LOCAL.</p>
 * <p>The main functionality is implemented in the connected providers.</p>
 */
@CompileStatic
class SetLocalVersion extends DefaultTask {

    /**
     * Version recommendation provider of this task
     */
    @Input
    RecommendationProvider provider

    /**
     * Task action
     */
    @TaskAction
    void runSetLocalExtension() {
        if(provider.isVersionRequired() && ! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        provider.setVersionExtension(VersionExtension.LOCAL)
    }

    /**
     * Description
     *
     * @return "Extend filter configuration for 'provider name' with LOCAL"
     */
    @Override
    String getDescription() {
        return "Extend filter configuration for ${provider.getName()} with LOCAL"
    }

    /**
     * Group
     *
     * @return "Provider name - Version Recommendation"
     */
    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}
