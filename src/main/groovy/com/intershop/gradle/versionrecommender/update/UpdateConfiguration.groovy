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

import com.intershop.gradle.versionrecommender.util.UpdatePos
import groovy.transform.CompileStatic
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

@CompileStatic
class UpdateConfiguration {

    private VersionUpdater updater

    String ivyPattern

    List<String> defaultUpdateProvider = []

    String defaultUpdate = UpdatePos.HOTFIX.toString()

    UpdatePos getUpdatePos() {
        return defaultUpdate as UpdatePos
    }

    final NamedDomainObjectContainer<UpdateConfigurationItem> updateConfigItemContainer

    UpdateConfiguration(Project project) {
        updater = new VersionUpdater(project)
        updateConfigItemContainer = project.container(UpdateConfigurationItem)
    }

    void updateConfigItemContainer(Closure c) {
        updateConfigItemContainer.configure(c)
    }

    void addConfigurationItem(UpdateConfigurationItem item) {
        updateConfigItemContainer.add(item)
    }

    String getUpdate(String group, String name, String version) {
        String returnValue = null

        UpdateConfigurationItem config = getConfigItem(group, name)
        updater.ivyPattern = ivyPattern

        if(config.searchPattern) {
            returnValue = updater.getUpdateVersion(group, name, version, config.searchPattern, config.updatePos, config.getVersionPattern())
        } else if(config.patternForNextVersion) {
            returnValue = updater.getUpdateVersion(group, name, version, config.patternForNextVersion, config.sortStringPos)
        } else {
            returnValue = updater.getUpdateVersion(group, name, version, config.updatePos)
        }
        return returnValue
    }

    UpdateConfigurationItem getConfigItem(String searchOrg, String searchModule) {
        UpdateConfigurationItem item = updateConfigItemContainer.sort().find {
            ((it.org && searchOrg =~ /${it.org.replaceAll('\\.', '\\\\.').replaceAll('\\*', '.*?')}/) || (! it.org)) &&
                    ((it.module && searchModule =~ /${it.module.replaceAll('\\*', '.*?')}/) || (! it.module))
        }
        if(item == null) {
            item = new UpdateConfigurationItem('empty')
        }
        return item
    }
}
