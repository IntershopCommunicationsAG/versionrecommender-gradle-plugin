package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SetLocalVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void runSetLocalExtension() {
        if(provider.isVersionRequired() && ! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        provider.setVersionExtension(VersionExtension.LOCAL)
    }

    @Override
    String getDescription() {
        return "Extend filter configuration for ${provider.getName()} with LOCAL"
    }

    @Override
    String getGroup() {
        return "Version Recommendation for ${provider.getName()}"
    }
}
