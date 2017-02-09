package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.util.UpdatePos
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project

@CompileStatic
@Slf4j
abstract class AbstractVersionProvider implements VersionProvider {

    protected VersionExtension versionExtension

    protected File workingDir
    protected File configDir

    protected UpdatePos updatePos
    protected Map<String, String> versions = null

    String name
    Project project

    protected boolean transitive
    protected boolean overrideTransitives

    AbstractVersionProvider(String name, Project project) {
        this.updatePos = UpdatePos.NONE

        setWorkingDir(new File(project.getRootProject().getBuildDir(), VersionRecommenderExtension.EXTENSIONNAME))
        setConfigDir(project.getRootProject().getProjectDir())

        this.name = name
        this.project = project
    }

    abstract void fillVersionMap()

    @Override
    void setUpdatePos(String updatePos) {
        try {
            this.updatePos = UpdatePos.valueOf(updatePos.toUpperCase())
        } catch (Exception ex) {
            log.warn('Update position was set to NONE, because the {} is not a valid value.', updatePos)
            this.updatePos = UpdatePos.NONE
        }
    }

    // Working Dir
    @Override
    void setWorkingDir(File workingDir) {
        this.workingDir = workingDir
        if(! workingDir.exists())
            workingDir.mkdirs()
    }

    // Config Dir
    @Override
    void setConfigDir(File configDir) {
        this.configDir = configDir
        if(! configDir.exists())
            configDir.mkdirs()
    }

    @Override
    void setVersionExtension(VersionExtension versionExtension) {
        this.versionExtension = versionExtension
    }

    @Override
    String getVersion(String org, String name) {
        fillVersionMap()
        if(versions != null)
            return versions.get("${org}:${name}".toString())

        return null
    }

    @Override
    void overrideTransitives(boolean override){
        this.overrideTransitives = override
    }

    @Override
    void useTransitives(boolean transitive){
        this.transitive = transitive
    }
}
