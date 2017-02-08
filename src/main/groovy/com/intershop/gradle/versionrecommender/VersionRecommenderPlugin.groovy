package com.intershop.gradle.versionrecommender

import com.intershop.gradle.versionrecommender.extension.RecommendationProvider
import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.provider.*
import com.intershop.gradle.versionrecommender.util.FileInputType
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolveDetails


class VersionRecommenderPlugin implements Plugin<Project> {

    private VersionRecommenderExtension extension
    Map<String, VersionProvider> providerMap = [:]

    @Override
    void apply(final Project project) {
        project.logger.info('Create extension {} for {}', VersionRecommenderExtension.EXTENSIONNAME, project.name)
        extension = project.extensions.findByType(VersionRecommenderExtension) ?: project.extensions.create(VersionRecommenderExtension.EXTENSIONNAME, VersionRecommenderExtension, project)

        applyRecommendation(project)
    }


    private void applyRecommendation(Project project) {
        project.getConfigurations().all { Configuration conf ->
            conf.getResolutionStrategy().eachDependency { DependencyResolveDetails details ->
                String rv = ''

                extension.provider.any { RecommendationProvider rp ->
                    if(providerMap.containsKey(rp.getName()) ) {
                        rv = providerMap.get(rp.getName()).getVersion(details.requested.group, details.requested.name)
                        return rv
                    } else {
                        VersionProvider vp = getProviderFromExtension(project, rp)
                        providerMap.put(rp.getName(), vp)
                        rv = vp.getVersion(details.requested.group, details.requested.name)
                        return rv
                    }
                }
                details.useVersion(rv)
            }
        }
    }

    private VersionProvider getProviderFromExtension(Project project, RecommendationProvider rp) {
        String i = ''
        FileInputType t = null
        VersionProvider vp = null

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
                vp = new IvyProvider(rp.getName(), project, i, t)
                break
            case 'pom':
                vp = new MavenProvider(rp.getName(), project, i, t)
                break
            case 'properties':
                vp = new PropertiesProvider(rp.getName(), project, i, t)
                vp.setVersionList(rp.getVersionList())
                vp.setUpdateExceptions(rp.getUpdateExceptions())
                break
        }
        return vp
    }
}