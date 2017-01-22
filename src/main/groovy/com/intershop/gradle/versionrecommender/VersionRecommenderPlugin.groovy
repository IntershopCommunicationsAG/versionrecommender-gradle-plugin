package com.intershop.gradle.versionrecommender

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.internal.reflect.Instantiator

import javax.inject.Inject

class VersionRecommenderPlugin implements Plugin<Project> {

    private VersionRecommenderExtension extension
    private final Instantiator instantiator

    @Inject
    VersionRecommenderPlugin(Instantiator instantiator) {
        this.instantiator = instantiator
    }

    @Override
    void apply(final Project project) {

        project.logger.info('Create extension {} for {}', VersionRecommenderExtension.EXTENSIONNAME, project.name)
        extension = project.extensions.findByType(VersionRecommenderExtension) ?: project.extensions.create(VersionRecommenderExtension.EXTENSIONNAME, VersionRecommenderExtension, project, instantiator)

    }

    private void applyRecommendations(final Project project) {
        project.getConfigurations().all { Configuration conf ->
            conf.getIncoming().beforeResolve { ResolvableDependencies resDep ->
                for (Dependency dependency : resDep.getDependencies()) {

                }
            }

            conf.getResolutionStrategy().eachDependency {DependencyResolveDetails depResDetails ->
                ModuleVersionSelector requested = depResDetails.getRequested()

                // don't interfere with the way forces trump everything
                for (ModuleVersionSelector force : conf.getResolutionStrategy().getForcedModules()) {
                    if (requested.getGroup().equals(force.getGroup()) && requested.getName().equals(force.getName())) {
                        return
                    }
                }

                String version = getRecommendedVersionRecursive(project, requested)
                if(rsFactory.getRecommendationStrategy().recommendVersion(depResDetails, version)) {
                    logger.info("Recommending version " + version + " for dependency " + requested.getGroup() + ":" + requested.getName())
                }
            }
        }
    }

    /**
     * Look for recommended versions in a project and each of its ancestors in order until one is found or the root is reached
     * @return the recommended version or <code>null</code>
     */
    protected String getRecommendedVersionRecursive(Project project, ModuleVersionSelector mvSelector) {
        String version = project.getExtensions().getByType(VersionRecommenderExtension.class)
                .getRecommendedVersion(mvSelector.getGroup(), mvSelector.getName())

        if (version != null)
            return version
        if (project.getParent() != null)
            return getRecommendedVersionRecursive(project.getParent(), mvSelector)

        project.logger.error('No version available for {}.', mvSelector.toString())
        return null
    }
}