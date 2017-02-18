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
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CompileStatic
class StoreUpdateVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @OutputFile
    File versionFile

    @TaskAction
    void storeUpdateVersion(){
        try {
            versionFile = provider.store(getVersionFile())
        }catch (IOException ex) {
            throw new GradleException('It was not possible to store changes!')
        }
    }

    @Override
    String getDescription() {
        return "Store changes from working dir for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}
