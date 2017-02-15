package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResetVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void resetVersion() {
        provider.setVersionExtension(VersionExtension.NONE)
    }

    @Override
    String getDescription() {
        return "Reset filter configuration for ${provider.getName()}"
    }

    @Override
    String getGroup() {
        return "Version Recommendation for ${provider.getName()}"
    }
}

