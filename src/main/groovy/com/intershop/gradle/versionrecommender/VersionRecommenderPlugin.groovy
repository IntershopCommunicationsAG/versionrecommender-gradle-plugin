package com.intershop.gradle.versionrecommender

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.util.NoVersionException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.ivy.plugins.IvyPublishPlugin
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.plugins.MavenPublishPlugin
import org.gradle.api.specs.Spec

class VersionRecommenderPlugin implements Plugin<Project> {

    private VersionRecommenderExtension extension

    @Override
    void apply(final Project project) {
        applyToRootProject(project.getRootProject())
    }

    private void applyToRootProject(Project project) {
        project.logger.info('Create extension {} for {}', VersionRecommenderExtension.EXTENSIONNAME, project.name)

        extension = project.extensions.findByType(VersionRecommenderExtension) ?: project.extensions.create(VersionRecommenderExtension.EXTENSIONNAME, VersionRecommenderExtension, project)

        applyRecommendation(project)
        applyIvyVersionRecommendation(project)
        applyMvnVersionRecommendation(project)

        project.getSubprojects().each {
            applyRecommendation(it)
            applyIvyVersionRecommendation(it)
            applyMvnVersionRecommendation(it)
        }
    }

    private void applyIvyVersionRecommendation(Project project) {
        project.plugins.withType(IvyPublishPlugin) {
            project.publishing {
                publications.withType(IvyPublication) {
                    IvyPublication publication = delegate

                    descriptor.withXml { XmlProvider xml ->
                        def rootNode = xml.asNode()
                        def dependenciesWithoutRevAttribute = rootNode.dependencies.dependency.findAll { !it.@rev }

                        dependenciesWithoutRevAttribute.each { dependencyNode ->
                            def configurationName = (dependencyNode.@conf).split('->')[0]

                            def configuration = project.configurations.findByName(configurationName)

                            if (!configuration) {
                                project.logger.warn("Failed to provide 'rev' attribute for dependency '{}:{}' in publication '{}' as there is no  project configuration of the name '{}'",
                                        dependencyNode.@org, dependencyNode.@name, publication.name, configurationName)
                                return
                            }

                            def resolvedDependencies = configuration.resolvedConfiguration.getFirstLevelModuleDependencies ({ Dependency resolvedDependency ->
                                resolvedDependency.name == dependencyNode.@name && resolvedDependency.group == dependencyNode.@org
                            } as Spec<Dependency>)

                            if (resolvedDependencies.size() == 0) {
                                project.logger.warn("Failed to provide 'rev' attribute for dependency '{}:{}' in publication '{}' as there is no dependency of that name in resolved project configuration '{}'",
                                        dependencyNode.@org, dependencyNode.@name, publication.name, configurationName)
                                return
                            }

                            ResolvedDependency resolvedDependency = (resolvedDependencies as List)[0]
                            dependencyNode.@rev = resolvedDependency.module.id.version
                        }
                    }
                }
            }
        }
    }

    private void applyMvnVersionRecommendation(Project project) {

        project.plugins.withType(MavenPublishPlugin) {
            project.publishing {
                publications {
                    withType(MavenPublication) {
                        MavenPublication publication = delegate

                        pom.withXml { XmlProvider xml ->
                            def rootNode = xml.asNode()
                            def dependenciesWithoutVersion = rootNode.dependencies.dependency.findAll {
                                !it.version.text()
                            }

                            def dependenciesWithoutVersionFromMgmt = rootNode.dependencyManagement.dependencies.dependency.findAll {
                                !it.version.text()
                            }

                            dependenciesWithoutVersion.addAll(dependenciesWithoutVersionFromMgmt)

                            dependenciesWithoutVersion.each { dependencyNode ->
                                def configurationName = dependencyNode.scope.text()
                                def configuration = project.configurations.findByName(configurationName)

                                if (!configuration) {
                                    project.logger.warn("Failed to provide 'version' attribute for dependency '{}:{}' in publication '{}' as there is no  project configuration of the name '{}'",
                                            dependencyNode.groupId.text(), dependencyNode.artifactId.text(), publication.name, configurationName)
                                    return
                                }

                                def resolvedDependencies = configuration.resolvedConfiguration.getFirstLevelModuleDependencies({ Dependency resolvedDependency ->
                                    resolvedDependency.name == dependencyNode.artifactId.text() && resolvedDependency.group == dependencyNode.groupId.text()
                                } as Spec<Dependency>)

                                if (resolvedDependencies.size() == 0) {
                                    project.logger.warn("Failed to provide 'version' attribute for dependency '{}:{}' in publication '{}' as there is no dependency of that name in resolved project configuration '{}'",
                                            dependencyNode.artifactId.text(), dependencyNode.artifactId.text(), publication.name, configurationName)
                                    return
                                }

                                ResolvedDependency resolvedDependency = (resolvedDependencies as List)[0]
                                dependencyNode.appendNode((new Node(null, 'version'))).setValue(resolvedDependency.module.id.version)
                            }
                        }
                    }
                }
            }
        }

    }

    private void applyRecommendation(Project project) {
        project.getConfigurations().all { Configuration conf ->
            conf.getResolutionStrategy().eachDependency { DependencyResolveDetails details ->
                if(! details.requested.version || extension.forceRecommenderVersion) {
                    String rv = extension.provider.getVersion(details.requested.group, details.requested.name)

                    if(details.requested.version && !(rv))
                        rv = details.requested.version

                    if(rv) {
                        details.useVersion(rv)
                    } else {
                        throw new NoVersionException("Version for '${details.requested.group}:${details.requested.name}' not found! Please check your dependency configuration and the version recommender version.")
                    }
                }
            }
        }
    }

}