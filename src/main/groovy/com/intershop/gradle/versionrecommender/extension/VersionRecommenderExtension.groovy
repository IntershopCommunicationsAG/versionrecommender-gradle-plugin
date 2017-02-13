package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import org.gradle.api.Project
import org.gradle.util.ConfigureUtil

class VersionRecommenderExtension {

    final static String EXTENSIONNAME = 'versionRecommendation'

    private Project project

    final RecommendationProviderContainer provider

    final String[] defaultUpdateConfigurations

    final UpdateConfiguration updateConfiguration

    VersionRecommenderExtension(Project project) {
        this.project = project
        provider = new RecommendationProviderContainer(project)

        updateConfiguration = new UpdateConfiguration(project)
        defaultUpdateConfigurations = []
    }

    void updateConfiguration(final Closure c) {
        project.configure(updateConfiguration, c)
    }

    void provider(Closure c) {
        ConfigureUtil.configure(c, provider)
    }

    boolean forceRecommenderVersion = false
}
