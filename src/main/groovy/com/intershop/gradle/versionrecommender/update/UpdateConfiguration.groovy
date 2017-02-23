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
package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.util.UpdatePos
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

/**
 * Extension for the configuration of
 * the project update configuration.
 */
@CompileStatic
class UpdateConfiguration {

    // the target project
    private Project project

    // Update log file
    private File updateLogFile

    // Version updater class
    private VersionUpdater updater

    /**
     * <p>Ivy layout pattern</p>
     * <p>If there is no configuration available,
     * no updates from IVY files will be calculated.</p>
     */
    String ivyPattern

    /**
     * A list of default recommendation providers which
     * will be updated if the task 'update' is called.
     * The list is also used by the 'store' task.
     */
    List<String> defaultUpdateProvider = []

    /**
     * Attribute for the update position for
     * all semantic versions.
     */
    String defaultUpdate = UpdatePos.HOTFIX.toString()

    /**
     * Update log file
     */
    void setUpdateLogFile(File logfile){
        this.updateLogFile = logfile
    }

    File getUpdateLogFile() {
        if(! updateLogFile) {
            updateLogFile = new File(project.getRootProject().getBuildDir(), "${VersionRecommenderExtension.EXTENSIONNAME}/update/update.log")
        }
        return updateLogFile
    }

    /**
     * Read access to the default update position.
     *
     * @return update position for semantic versions
     */
    UpdatePos getUpdatePos() {
        return defaultUpdate as UpdatePos
    }

    /**
     * Container of all configuration items.
     * The order of the items depends on the name.
     */
    final NamedDomainObjectContainer<UpdateConfigurationItem> updateConfigItemContainer

    /**
     * Constructor
     *
     * @param project the target project
     */
    UpdateConfiguration(Project project) {
        this.project = project

        updater = new VersionUpdater(this.project)
        updater.updateLogFile = getUpdateLogFile()

        updateConfigItemContainer = project.container(UpdateConfigurationItem)
    }

    /**
     * Initialize and configures the item container from a closure
     *
     * @param itemcontainer container configuration with items
     */
    void updateConfigItemContainer(Closure itemcontainer) {
        updateConfigItemContainer.configure(itemcontainer)
    }

    /**
     * Add an configuration item to the existing container.
     *
     * @param item update configuration item
     */
    void addConfigurationItem(UpdateConfigurationItem item) {
        updateConfigItemContainer.add(item)
    }

    /**
     * Returns the update version for the given module.
     *
     * @param group     Group of the provider configuration
     * @param name      Name of the provider configuration
     * @param version   Old version ot the provider configuration
     * @return          Updated version
     */
    String getUpdate(String group, String name, String version) {
        String returnValue = null

        UpdateConfigurationItem config = getConfigItem(group, name)
        updater.ivyPattern = ivyPattern

        if(config.searchPattern) {
            returnValue = updater.getUpdateVersion(group, name, version, config.searchPattern, config.updatePos, config.getVersionPattern())
        } else if(config.patternForNextVersion) {
            returnValue = updater.getUpdateVersion(group, name, version, config.patternForNextVersion, config.sortStringPos)
        } else {
            returnValue = updater.getUpdateVersion(group, name, version, config.getUpdatePos())
        }
        return returnValue
    }

    /**
     * Provides the first matching update configuration.
     * The order of the items depends on the name.
     * It is also possible to use the '*' pattern.
     *
     * @param searchGroup   Group of the provider configuration
     * @param searchName    Name of the provider configuration
     * @return              first matching update configuration
     */
    UpdateConfigurationItem getConfigItem(String searchGroup, String searchName) {
        UpdateConfigurationItem item = updateConfigItemContainer.sort().find {
            ((it.org && searchGroup =~ /${it.org.replaceAll('\\.', '\\\\.').replaceAll('\\*', '.*?')}/) || (! it.org)) &&
                    ((it.module && searchName =~ /${it.module.replaceAll('\\*', '.*?')}/) || (! it.module))
        }
        if(item == null) {
            item = new UpdateConfigurationItem('empty')
            item.setUpdate(getUpdatePos().toString())
        }
        return item
    }
}
