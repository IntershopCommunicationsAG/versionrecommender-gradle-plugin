package com.intershop.gradle.versionrecommender

import com.intershop.gradle.test.AbstractProjectSpec
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import org.gradle.api.Plugin

class VersionRecommenderPluginSpec extends AbstractProjectSpec {

    @Override
    Plugin getPlugin() {
        return new VersionRecommenderPlugin()
    }


    def 'should add extension named versionRecommender'() {
        when:
        plugin.apply(project)

        then:
        project.extensions.getByName(VersionRecommenderExtension.EXTENSIONNAME)
    }
}
