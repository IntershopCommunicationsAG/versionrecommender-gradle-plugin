package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.util.UpdatePos
import org.gradle.api.Named

class RecommendationProvider implements Named {

    RecommendationProvider(String name) {
        this.name = name
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
}
