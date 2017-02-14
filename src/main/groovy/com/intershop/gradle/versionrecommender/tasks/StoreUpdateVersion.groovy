package com.intershop.gradle.versionrecommender.tasks

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import groovy.transform.CompileStatic
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction

@CompileStatic
class StoreUpdateVersion extends DefaultTask {

    @Input
    RecommendationProvider provider

    @TaskAction
    void storeUpdateVersion(){
        try {
            provider.store()
        }catch (IOException ex) {
            throw new GradleException('It was not possible to store changes!')
        }
    }
}
