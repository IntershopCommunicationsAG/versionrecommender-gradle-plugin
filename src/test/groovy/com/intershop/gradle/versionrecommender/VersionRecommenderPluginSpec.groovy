package com.intershop.gradle.versionrecommender

import com.intershop.gradle.test.AbstractProjectSpec
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import org.gradle.api.Plugin
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry

class VersionRecommenderPluginSpec extends AbstractProjectSpec {

    @Override
    Plugin getPlugin() {
        ServiceRegistry services = (project as ProjectInternal).services
        Instantiator instantiator = services.get(Instantiator)

        return new VersionRecommenderPlugin(instantiator)
    }


    def 'should add extension named versionRecommender'() {
        when:
        plugin.apply(project)

        then:
        project.extensions.getByName(VersionRecommenderExtension.EXTENSIONNAME)
    }
}
