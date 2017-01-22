package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.provider.IvyProvider
import com.intershop.gradle.versionrecommender.provider.MavenProvider

import com.intershop.gradle.versionrecommender.provider.PropertiesProvider
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project

class VersionRecommenderExtension {

    final static String EXTENSIONNAME = 'versionRecommender'

    private Project project

    final NamedDomainObjectContainer<IvyProvider> ivy

    final NamedDomainObjectContainer<MavenProvider> maven

    final NamedDomainObjectContainer<PropertiesProvider> versionprops

    VersionRecommenderExtension(Project project) {
        this.project = project

        ivy = project.container(IvyProvider)
        maven = project.container(MavenProvider)
        versionprops = project.container(PropertiesProvider)
    }

    String[] filterOrder = []

    void ivy(Closure c) {
        ivy.configure(c)
    }

    void maven(Closure c) {
        maven.configure(c)
    }

    void versionprops(Closure c) {
        versionprops.configure(c)
    }

    void versionconfig(Closure c) {
        versionconfig.configure(c)
    }

    static String getVersion(String group, String name) {
        return ''
    }
}
