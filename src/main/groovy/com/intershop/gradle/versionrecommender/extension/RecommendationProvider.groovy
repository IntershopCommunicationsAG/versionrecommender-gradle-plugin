package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.provider.VersionProvider
import com.intershop.gradle.versionrecommender.util.UpdatePos
import org.gradle.api.Named

class RecommendationProvider implements Named {

    private VersionProvider versionProvider
    private String name

    RecommendationProvider(String name, VersionProvider versionProvider) {
        this.name = name
        this.versionProvider = versionProvider
    }

    String getName() {
        return this.name
    }

    boolean transitive = false

    String updatePos = UpdatePos.MINOR.toString()

    Map<String, String> versionList = null
    List<String> updateExceptions = null

    boolean transitiv = false

    boolean overrideTransitives = false

    File workingDir = null

    File configDir = null

    String getVersion(String org, String name) {
        versionProvider.getVersion(org, name)
    }
}
