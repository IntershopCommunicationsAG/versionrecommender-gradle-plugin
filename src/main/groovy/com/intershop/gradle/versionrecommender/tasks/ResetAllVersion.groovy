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
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

/**
 * <p>Reset task of a project</p>
 * <p>This task removes temporary version information of all providers
 * in the configured default list to the project configuration.
 * The main functionality is implemented in the connected providers.</p>
 */
@CompileStatic
class ResetAllVersion extends DefaultTask {

    /**
     * Provider list of this task
     */
    @Input
    List<RecommendationProvider> providers = []

    /**
     * Task action
     */
    @TaskAction
    void resetVersion() {
        HashSet<File> workingdirs = new HashSet<File>()
        providers.each { RecommendationProvider p ->
            workingdirs.add(p.getWorkingDir())
        }
        workingdirs.each {File wd ->
            project.logger.quiet('All files in {} are removed.', wd.absolutePath)
            wd.deleteDir()
            wd.mkdir()
        }
        providers.each { RecommendationProvider p ->
            p.initializeVersion()
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
        return "Reset all filter configurations"
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

