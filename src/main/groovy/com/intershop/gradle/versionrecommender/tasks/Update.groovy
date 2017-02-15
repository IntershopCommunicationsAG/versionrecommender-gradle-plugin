package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class Update extends DefaultTask {

    @Input
    List<RecommendationProvider> providers = []

    @TaskAction
    void runUpdate() {
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)

        providers.each {RecommendationProvider p ->
            if(ext.updateConfiguration.defaultUpdateProvider.contains(p.getName())) {
                if(p.isVersionRequired() && ! (p.getVersionFromProperty())) {
                    throw new GradleException("It is necessary to specify a version property with -P${p.getVersionPropertyName()} = <version>.")
                }
                p.update(ext.updateConfiguration)
            }
        }
    }

    @Override
    String getDescription() {
        return "Update Dependencies for configured default filters"
    }

    @Override
    String getGroup() {
        return "Version Recommendation"
    }
}
