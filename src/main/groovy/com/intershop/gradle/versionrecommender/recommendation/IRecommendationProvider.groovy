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
/**
 * Interface for all recommendation provider
 */
@CompileStatic
interface IRecommendationProvider extends Named {

    /**
     * Holds the name of the provider
     *
     * @return the name
     */
    @Override
    String getName()

    /**
     * Update the version of the provider with a
     * special update configuration.
     *
     * @param updateConfig the update configuration for this provider
     */
    void update(UpdateConfiguration updateConfig)

    /**
     * Overrides the version of the provider, if
     * isAdaptable is true.
     *
     * @throws IOException
     */
    void setVersion() throws IOException

    /**
     * Stores changed version information to
     * the project configuration, if
     * isAdaptable is true.
     *
     * @param Input file for the operation
     * @return File with version information
     * @throws IOException
     */
    File store(File outputFile) throws IOException

    /**
     * Initialize the list of a map with
     * coordinates and version.
     *
     * This must me called for an update of the list.
     */
    void initializeVersion()

    /**
     * Get file object with version information
     *
     * @return file with version information
     */
    File getVersionFile()

    /**
     * The working dir is used for all operations
     * with temporary files.
     *
     * @param workingDir
     */
    void setWorkingDir(File workingDir)

    /**
     * The config dir is the directory with
     * all stored project files.
     *
     * @param configDir
     */
    void setConfigDir(File configDir)

    /**
     * It is possible to add an extension
     * to the existing version, if
     * isAdaptable is true.
     *
     * @param vex version extension, eg SNAPSHOT or LOCAL
     */
    void setVersionExtension(VersionExtension vex)

    /**
     * If the result of this method is true,
     * it is possible to adapt the version
     *
     * @return true if it is possible to adapt the version
     */
    boolean isAdaptable()

    /**
     * If also transitive dependencies are to be added to the
     * filter list, the value must be set to true.
     *
     * @param transitive true for transitive resolution of configured dependency
     */
    void setTransitive(boolean transitive)

    /**
     * If a resolved dependency should override an
     * existing version, the value must be set to true.
     *
     * @param override
     */
    void setOverrideTransitiveDeps(boolean override)

    /**
     * Main method of the provider. This method should
     * return a version for an given module with name.
     *
     * @param org Organisiation or Group of the dependency
     * @param name Name or artifact id of the dependency
     * @return the version of the dependency from the provider.
     */
    String getVersion(String org, String name)
}
