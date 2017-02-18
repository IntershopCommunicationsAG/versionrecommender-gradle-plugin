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

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResetVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void resetVersion() {
        provider.setVersionExtension(VersionExtension.NONE)
    }

    @Override
    String getDescription() {
        return "Reset filter configuration for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}

