package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.update.UpdateConfigurationItem
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class UpdateConfigExtension {

    private Project project

    String ivyPattern

    final NamedDomainObjectContainer<UpdateConfigurationItem> updateConfigItemContainer

    UpdateConfigExtension(Project project) {
        this.project = project
        updateConfigItemContainer = project.container(UpdateConfigurationItem)
    }

    void updateConfigItemContainer(Closure c) {
        updateConfigItemContainer.configure(c)
    }
}
