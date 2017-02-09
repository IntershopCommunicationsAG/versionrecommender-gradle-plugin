package com.intershop.gradle.versionrecommender.extension

import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class VersionRecommenderExtension {

    final static String EXTENSIONNAME = 'versionRecommendation'

    private Project project

    final NamedDomainObjectContainer<RecommendationProvider> provider

    final String[] defaultUpdateConfigurations

    final UpdateConfigExtension updateConfiguration

    VersionRecommenderExtension(Project project) {
        this.project = project
        provider = project.container(RecommendationProvider, new RecommendationProviderFactory(project))

        updateConfiguration = new UpdateConfigExtension(project)
        defaultUpdateConfigurations = []
    }

    void updateConfiguration(final Closure c) {
        project.configure(updateConfiguration, c)
    }

    void provider(Closure c) {
        provider.configure(c)
    }

    boolean forceRecommenderVersion = false
}
