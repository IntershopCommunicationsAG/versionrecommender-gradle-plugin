package com.intershop.gradle.versionrecommender.recommendation

import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ModuleVersionSelector

class RecommendationStrategy {

    boolean canRecommendVersion(ModuleVersionSelector selector) {
        String version = selector.getVersion();
        return version == null || version.isEmpty();
    }

    boolean recommendVersion(DependencyResolveDetails details, String version) {
        if (version == null) {
            return false;
        }
        details.useVersion(version);
        return true;
    }
}
