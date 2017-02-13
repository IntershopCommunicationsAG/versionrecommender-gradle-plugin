package com.intershop.gradle.versionrecommender.update

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class UpdateConfiguration {

    private VersionUpdater updater

    String ivyPattern

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
            returnValue = updater.getUpdateVersion(group, name, version, config.patternForNextVersion, config.sortStringPos, config.getVersionPattern())
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
