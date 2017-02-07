package com.intershop.gradle.versionrecommender

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.provider.*
import com.intershop.gradle.versionrecommender.util.FileInputType
import org.gradle.api.Plugin
import org.gradle.api.Project

class VersionRecommenderPlugin implements Plugin<Project> {

    private VersionRecommenderExtension extension

    @Override
    void apply(final Project project) {
        project.logger.info('Create extension {} for {}', VersionRecommenderExtension.EXTENSIONNAME, project.name)
        extension = project.extensions.findByType(VersionRecommenderExtension) ?: project.extensions.create(VersionRecommenderExtension.EXTENSIONNAME, VersionRecommenderExtension, project)

        createVersionProvider(project)
    }

    private void createVersionProvider(Project project) {
        List<VersionProvider> providerList = []
        String i
        FileInputType t
        AbstractFileBasedProvider p

        extension.getProvider().each { RecommendationProvider rp ->

            input = ''
            type = null
            if(rp.getDependency()) {
                i = rp.getDependency()
                t = FileInputType.DEPENDENCYMAP
            } else if(rp.getFile()) {
                i = rp.getFile()
                t = FileInputType.FILE
            } else if(rp.getUrl()) {
                i = rp.getUrl()
                t = FileInputType.URL
            } else if(rp.getUri()) {
                i = rp.getUri()
                t = FileInputType.URI
            }

            switch (rp.getType()) {
                case 'ivy':
                    p = new IvyProvider(rp.getName(), project, i, t)
                    break
                case 'pom':
                    p = new MavenProvider(rp.getName(), project, i, t)
                    break
                case 'properties':
                    p = new PropertiesProvider(rp.getName(), project, i, t)
                    p.setVersionList(rp.getVersionList())
                    p.setUpdateExceptions(rp.getUpdateExceptions())
                    break
            }

            if(p) {
                providerList.add(p)
            }
        }
    }
}