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
            if(ext.defaultUpdateConfigurations.contains(p.getName())) {
                if(p.isVersionRequired() && ! (p.getVersionFromProperty())) {
                    throw new GradleException("It is necessary to specify a version property with -P${p.getVersionPropertyName()} = <version>.")
                }
                p.update(ext.updateConfiguration)
            }
        }
    }
}
