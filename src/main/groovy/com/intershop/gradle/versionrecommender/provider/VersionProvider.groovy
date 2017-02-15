package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import org.gradle.api.Named

@CompileStatic
interface VersionProvider extends Named {

    @Override
    String getName()

    // Update methods
    void setUpdatePos(String updatePos)

    void update(UpdateConfiguration updateConfig)

    File store(File outputFile) throws java.io.IOException

    File getVersionFile()

    // Working Dir
    void setWorkingDir(File workingDir)

    // Config Dir
    void setConfigDir(File configDir)

    void setVersionExtension(VersionExtension vex)

    boolean isAdaptable()

    void useTransitives(boolean transitive)

    void overrideTransitives(boolean override)

    String getVersion(String org, String name) throws Exception
}
