package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class ResetAllVersion extends DefaultTask {

    @Input
    List<RecommendationProvider> providers = []

    @TaskAction
    void resetVersion() {
        HashSet<File> workingdirs = new HashSet<File>()
        providers.each { RecommendationProvider p ->
            workingdirs.add(p.getWorkingDir())
        }
        workingdirs.each {File wd ->
            project.logger.quiet('All files in {} are removed.', wd.absolutePath)
            wd.deleteDir()
            wd.mkdir()
        }
    }

    @Override
    String getDescription() {
        return "Reset all filter configurations"
    }

    @Override
    String getGroup() {
        return "Version Recommendation"
    }
}

