package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction

@CompileStatic
class StoreUpdate extends DefaultTask {

    @Input
    List<RecommendationProvider> providers = []

    @OutputFiles
    List<File> versionFiles

    @TaskAction
    void storeChanges() {
        VersionRecommenderExtension ext = project.extensions.findByType(VersionRecommenderExtension)
        versionFiles = []

        providers.each {RecommendationProvider p ->
            if(ext.defaultUpdateConfigurations.contains(p.getName())) {
                try {
                    File vf = p.store()
                    if(vf != null) {
                        versionFiles.add(vf)
                    }
                }catch (IOException ex) {
                    throw new GradleException("It was not possible to store changes of '${p.getName()}' (${ex.getMessage()})!")
                }
            }
        }
    }
}
