package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.provider.VersionProvider
import com.intershop.gradle.versionrecommender.util.UpdatePos
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project


@CompileStatic
@Slf4j
abstract class RecommendationProvider implements VersionProvider{

    private String name
    private File workingDir
    private File configDir

    protected boolean transitive = false
    protected boolean override = false
    protected Project project
    protected VersionExtension versionExtension
    protected UpdatePos updatePos
    protected Map<String, String> versions = null

    RecommendationProvider(String name, Project project) {
        this.name = name
        this.project = project

        this.updatePos = UpdatePos.NONE

        setWorkingDir(new File(project.getRootProject().getBuildDir(), VersionRecommenderExtension.EXTENSIONNAME))
        setConfigDir(project.getRootProject().getProjectDir())
    }

    String getName() {
        return this.name
    }

    @Override
    boolean isAdaptable() {
        return false
    }

    @Override
    void useTransitives(boolean transitive) {
        this.transitive = transitive
    }

    @Override
    void overrideTransitives(boolean override) {
        this.override = override
    }

    @Override
    void setUpdatePos(String updatePos) {
        try {
            this.updatePos = UpdatePos.valueOf(updatePos.toUpperCase())
        } catch (Exception ex) {
            log.warn('Update position was set to NONE, because the {} is not a valid value.', updatePos)
            this.updatePos = UpdatePos.NONE
        }
    }

    Map<String, String> versionList = null
    List<String> updateExceptions = null


    @Override
    void setVersionExtension(VersionExtension versionExtension) {
        this.versionExtension = versionExtension
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

    File getWorkingDir() {
        return this.workingDir
    }

    File getConfigDir() {
        return this.configDir
    }

    abstract void fillVersionMap()

    @Override
    String getVersion(String org, String name) {
        fillVersionMap()
        if(versions != null)
            return versions.get("${org}:${name}".toString())

        return null
    }
}
