package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.update.UpdateConfigurationItem
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class UpdateConfigExtension {

    private Project project

    UpdateConfigExtension(Project project) {
        this.project = project
        updateConfigItem = project.container(UpdateConfigurationItem)
    }

    String ivyPattern

    final NamedDomainObjectContainer<UpdateConfigurationItem> updateConfigItem

    void updateConfigItem(Closure c) {
        updateConfigItem.configure(c)
    }
}
