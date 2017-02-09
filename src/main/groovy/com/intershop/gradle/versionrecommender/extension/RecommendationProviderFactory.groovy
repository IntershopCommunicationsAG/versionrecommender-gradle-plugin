package com.intershop.gradle.versionrecommender.extension

import org.gradle.api.NamedDomainObjectFactory
import org.gradle.api.Project

class RecommendationProviderFactory implements NamedDomainObjectFactory<RecommendationProvider> {

    private Project project

    public RecommendationProviderFactory(Project project) {
        this.project = project
    }

    @Override
    RecommendationProvider create(String name) {
        RecommendationProvider provider = new RecommendationProvider(name, project)
        return provider
    }
}