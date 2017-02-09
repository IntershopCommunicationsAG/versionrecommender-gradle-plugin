package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.provider.IvyProvider
import com.intershop.gradle.versionrecommender.provider.MavenProvider
import com.intershop.gradle.versionrecommender.provider.PropertiesProvider
import com.intershop.gradle.versionrecommender.provider.VersionProvider
import com.intershop.gradle.versionrecommender.util.ConfigurationException
import com.intershop.gradle.versionrecommender.util.FileInputType
import com.intershop.gradle.versionrecommender.util.UpdatePos
import org.gradle.api.Named
import org.gradle.api.Project

class RecommendationProvider implements Named {

    private VersionProvider versionProvider
    private Project project

    RecommendationProvider(String name, Project project) {
        this.name = name
        this.project = project
    }

    String name

    String type

    boolean transitive = false

    String updatePos = UpdatePos.MINOR.toString()

    String dependency

    String file

    String url

    String uri

    Map<String, String> versionList = null
    List<String> updateExceptions = null

    String getVersion(String org, String name) {
        if(versionProvider == null) {
            initVersionProvider()
        }
        versionProvider.getVersion(org, name)
    }

    private void initVersionProvider() {
        String inputStr = ''
        FileInputType inputType = null

        if(dependency) {
            inputStr = getDependency()
            inputType = FileInputType.DEPENDENCYMAP
        } else if(file) {
            inputStr = getFile()
            inputType = FileInputType.FILE
        } else if(url) {
            inputStr = getUrl()
            inputType = FileInputType.URL
        } else if(uri) {
            inputStr = getUri()
            inputType = FileInputType.URI
        } else {
            throw new ConfigurationException('Please specify one input parameter - dependency, file, ulf, uri!')
        }

        switch (type) {
            case 'ivy':
                versionProvider = new IvyProvider(getName(), project, inputStr, inputType)
                break
            case 'pom':
                versionProvider = new MavenProvider(getName(), project, inputStr, inputType)
                break
            case 'properties':
                versionProvider = new PropertiesProvider(getName(), project, inputStr, inputType)
                versionProvider.setVersionList(getVersionList())
                versionProvider.setUpdateExceptions(getUpdateExceptions())
                break
        }
    }
}
