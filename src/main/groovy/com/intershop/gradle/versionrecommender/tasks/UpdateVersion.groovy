package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class UpdateVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void runUpdate() {
        if(provider.isVersionRequired() && ! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)
        provider.update(ext.updateConfiguration)
    }

    @Override
    String getDescription() {
        return "Update Dependencies for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "${provider.getName()} - Version Recommendation"
    }
}
