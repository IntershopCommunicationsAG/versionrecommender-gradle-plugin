package com.intershop.gradle.versionrecommender.update

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class UpdateConfiguration {

    private VersionUpdater updater

    private List<UpdateConfigurationItem> configItems

    UpdateConfiguration(Project project) {
        updater = new VersionUpdater(project)
        configItems = []
    }

    UpdateConfiguration(Project project, String ivyPattern) {
        updater = new VersionUpdater(project)
        updater.ivyPattern = ivyPattern
        configItems = []
    }

    void addConfigurationItem(UpdateConfigurationItem item) {
        configItems.add(item)
    }

    void addConfigurationItem(List<UpdateConfigurationItem> items) {
        configItems.addAll(items)
    }

    void addConfigurationItem(NamedDomainObjectContainer<UpdateConfigurationItem> items) {
        configItems.addAll(items.asList())
    }

    List<UpdateConfigurationItem> getConfigurationItems() {
        return configItems
    }

    String getUpdate(String group, String name, String version) {
        String returnValue = null
        UpdateConfigurationItem config = getConfigItem(group, name)

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
        UpdateConfigurationItem item = configItems.sort().find {
            ((it.org && searchOrg =~ /${it.org.replaceAll('\\.', '\\\\.').replaceAll('\\*', '.*?')}/) || (! it.org)) &&
                    ((it.module && searchModule =~ /${it.module.replaceAll('\\*', '.*?')}/) || (! it.module))
        }
        if(item == null) {
            item = new UpdateConfigurationItem('empty')
        }
        return item
    }
}
