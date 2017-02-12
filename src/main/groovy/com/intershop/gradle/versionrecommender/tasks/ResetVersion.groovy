package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.util.VersionExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class ResetVersion extends DefaultTask {

    RecommendationProvider provider

    @TaskAction
    void resetVersion() {
        provider.setVersionExtension(VersionExtension.NONE)
    }
}

