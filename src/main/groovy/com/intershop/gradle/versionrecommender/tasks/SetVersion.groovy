package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class SetVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void runSetLocalExtension() {
        if(! (provider.getVersionFromProperty())) {
            throw new GradleException("It is necessary to specify a version property with -P${provider.getVersionPropertyName()} = <version>.")
        }
        provider.setVersion()
    }

    @Override
    String getDescription() {
        return "Set special version for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "Version Recommendation for ${provider.getName()}"
    }
}
