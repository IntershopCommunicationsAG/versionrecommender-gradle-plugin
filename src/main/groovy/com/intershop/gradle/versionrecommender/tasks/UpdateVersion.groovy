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

@CompileStatic
class UpdateVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void runUpdate() {
        if(provider.isVersionRequired() && ! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)
        provider.update(ext.updateConfiguration)
    }

    @Override
    String getDescription() {
        return "Update Dependencies for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}
