package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SetSnapshotVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void runSetSnapshotExtension() {
        if(provider.isVersionRequired() && ! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        provider.setVersionExtension(VersionExtension.SNAPSHOT)
    }

    @Override
    String getDescription() {
        return "Extend filter configuration for ${provider.getName()} with SNAPSHOT"
    }

    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}
