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
package com.intershop.gradle.versionrecommender.recommendation

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.Named

@CompileStatic
interface VersionProvider extends Named {

    @Override
    String getName()

    void update(UpdateConfiguration updateConfig)

    void setVersion() throws java.io.IOException

    File store(File outputFile) throws java.io.IOException

    File getVersionFile()

    // Working Dir
    void setWorkingDir(File workingDir)

    // Config Dir
    void setConfigDir(File configDir)

    void setVersionExtension(VersionExtension vex)

    boolean isAdaptable()

    void setTransitives(boolean transitive)

    void setOverrideTransitives(boolean override)

    String getVersion(String org, String name) throws Exception
}
