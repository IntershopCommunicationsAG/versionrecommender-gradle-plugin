/*
 * Copyright 2015 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.intershop.gradle.versionrecommender.extension

import com.intershop.gradle.versionrecommender.recommendation.RecommendationProviderContainer
import com.intershop.gradle.versionrecommender.util.ModuleNotationParser
import groovy.transform.CompileStatic
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.publish.Publication
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication

/**
 * This is an extension for the publishing of an
 * filter creation.
 *
 * The methods must be called inside of the
 * publication.
 *
 * Ivy
 * <pre>
 * <code>
 *  publishing {
 *      publications {
 *          ivyFilter(IvyPublication) {
 *              module 'ivy-filter'
 *              revision project.version
 *
 *              versionManagement.withSubProjects { subprojects }
 *          }
 *      }
 * }
 * </pre>
 * </code>
 * Maven BOM
 * <pre>
 * <code>
 * publishing {
 *     publications {
 *         mvnFilter(MavenPublication) {
 *             artifactId 'mvn-filter'
 *             version project.version
 *
 *             versionManagement.withSubProjects { subprojects }
 *         }
 *    }
 * }
 * </pre>
 * </code>
 */
@CompileStatic
class PublicationXmlGenerator {

    final static String EXTENSIONNAME = 'versionManagement'

    private Project project
    private RecommendationProviderContainer providerContainer

    /**
     * Constructor
     *
     * @param project The target project
     */
    PublicationXmlGenerator(Project project) {
        this.project = project
    }

    /**
     * Method adds a list of sub projects or a single
     * project as dependencies to the descriptor.
     *
     * @param projectClosure List of sub projects / single project
     */
    void withSubProjects(Closure projectClosure) {
        Iterable<Project> subprojects = null

        def projectsRet = projectClosure()

        if(projectsRet instanceof Project) {
            subprojects = [(Project) projectsRet]
        } else if(Iterable.class.isAssignableFrom(projectsRet.class)) {
            subprojects = (Iterable<Project>)projectsRet
        }

        generateDependencyXmlBase((Publication)((Closure)projectClosure.delegate).delegate,
                { (List<ModuleVersionIdentifier>)subprojects.collect { Project p -> ModuleNotationParser.parse(p) } })
    }

    /**
     * Method adds a list of dependencies from
     * a list of configurations or a single configuration
     * to the descriptor.
     *
     * @param configurationsClosure List of configurations / single configuration
     */
    void fromConfigurations(Closure configurationsClosure) {
        Iterable<Configuration> configurations

        def configurationsRet = configurationsClosure()

        if(configurationsRet instanceof Configuration) {
            configurations = [(Configuration) configurationsRet]
        }else if(Iterable.class.isAssignableFrom(configurationsRet.class)) {
            configurations = (Iterable<Configuration>)configurationsRet
        }
        generateDependencyXmlBase((Publication)((Closure)configurationsClosure.delegate).delegate,
                { (List<ModuleVersionIdentifier>)configurations.collect { Configuration config -> getManagedDependencies(config) }.flatten() })
    }

    /**
     * Method adds a list of dependencies or a
     * single configuration dependency to the
     * descriptor.
     *
     * @param dependenciesClosure List of dependencies / single dependency
     */
    void withDependencies(Closure dependenciesClosure) {
        Iterable<String> dependencies = null

        def dependenciesRet = dependenciesClosure()

        if(dependenciesRet instanceof String) {
            dependencies = [(String) dependenciesRet]
        } else if(Iterable.class.isAssignableFrom(dependenciesRet.class)) {
            dependencies = (Iterable<String>)dependenciesRet
        }
        generateDependencyXmlBase((Publication)((Closure)dependenciesClosure.delegate).delegate,
                { (List<ModuleVersionIdentifier>)dependencies.collect {String dep ->  ModuleNotationParser.parse(dep) } })
    }

    /**
     * This is a helper method to add always the version recommendation exception.
     *
     * @param pub   Publication - Maven or Ivy
     * @param deps  Dependency closure
     */
    private void generateDependencyXmlBase(Publication pub, Closure<List<ModuleVersionIdentifier>> deps) {
        Object ext = project.getExtensions().findByName(VersionRecommenderExtension.EXTENSIONNAME)
        if(ext) {
            if (pub instanceof IvyPublication) {
                generateDependencyXml((IvyPublication) pub, deps, ((VersionRecommenderExtension) ext).provider)
            } else if (pub instanceof MavenPublication) {
                generateDependencyXml((MavenPublication) pub, deps, ((VersionRecommenderExtension) ext).provider)
            }
            else {
                throw new GradleException("This is not a Publication ${pub}!")
            }
        } else {
            throw new GradleException("Extension + ${VersionRecommenderExtension.EXTENSIONNAME} is missing!")
        }
    }

    /**
     * Method add dependencies to the descriptor.
     *
     * @param pub   IvyPublication
     * @param deps  Dependencies
     * @param rpc   RecommendationProviderContainer
     */
    protected static void generateDependencyXml(IvyPublication pub, Closure<List<ModuleVersionIdentifier>> deps, RecommendationProviderContainer rpc) {
        pub.descriptor.withXml(
            new Action<XmlProvider>() {
                @Override
                void execute(XmlProvider xmlProvider) {
                    Node root = xmlProvider.asNode()
                    Object findDep = root.children().find { it instanceof Node && ((Node)it).name() == 'dependencies' }

                    Node dependencies = findDep ? (Node)findDep : root.appendNode('dependencies')
                    dependencies.attributes().put('defaultconfmapping', '*->default')

                    deps.call().each { ModuleVersionIdentifier mvid ->
                        Map dependencyAttributes = [org: mvid.group, name: mvid.name]
                        if(mvid.getVersion()){
                            dependencyAttributes['rev'] = mvid.getVersion()
                        } else {
                            String version = rpc.getVersion(mvid.group, mvid.name)
                            if(version)
                                dependencyAttributes['rev'] = version
                        }
                        dependencyAttributes['conf'] = 'default'
                        dependencies.appendNode('dependency', dependencyAttributes)
                    }
                }
            }
        )
    }

    /**
     * Method add dependencies to the descriptor.
     *
     * @param pub   MavenPublication
     * @param deps  Dependencies
     * @param rpc   RecommendationProviderContainer
     */
    protected static void generateDependencyXml(MavenPublication pub, Closure<List<ModuleVersionIdentifier>> deps, RecommendationProviderContainer rpc) {
        pub.pom.withXml(
            new Action<XmlProvider>() {
                @Override
                void execute(XmlProvider xmlProvider) {
                    Node root = xmlProvider.asNode()
                    Object findDepMgt = root.children().find { it instanceof Node && ((Node)it).name() == 'dependencyManagement' }
                    // when merging two or more sources of dependencies, we want to only create one dependencyManagement section
                    Node dependencyManagement = findDepMgt ? (Node)findDepMgt : root.appendNode('dependencyManagement')


                    Object findDep = dependencyManagement.children().find { it instanceof Node && ((Node)it).name() == 'dependencies' }
                    Node dependencies = findDep ? (Node)findDep : dependencyManagement.appendNode("dependencies")

                    deps.call().each { ModuleVersionIdentifier mvid ->
                        Node dep = dependencies.appendNode("dependency")
                        dep.appendNode("groupId").value = mvid.group
                        dep.appendNode("artifactId").value = mvid.name
                        if(mvid.version) {
                            dep.appendNode("version").value = mvid.version
                        } else {
                            String version = rpc.getVersion(mvid.group, mvid.name)
                            if(version)
                                dep.appendNode("version").value = version
                        }
                    }
                }
            }
        )
    }

    /**
     * Calculates managed dependencies
     *
     * @param configuration
     * @return a set of ModuleVersionIdentifier
     */
    protected static Set<ModuleVersionIdentifier> getManagedDependencies(Configuration configuration) {
        getManagedDependenciesRecursive(configuration.resolvedConfiguration.firstLevelModuleDependencies,
                new HashSet<ModuleVersionIdentifier>())
    }

    /**
     * Calculates managed dependencies - recursive
     *
     * @param deps  set of ResolvedDependency
     * @param all   set of ModuleVersionIdentifier
     * @return a set of ModuleVersionIdentifier
     */
    protected static Set<ModuleVersionIdentifier> getManagedDependenciesRecursive(Set<ResolvedDependency> deps, Set<ModuleVersionIdentifier> all) {
        deps.each {ResolvedDependency dep ->
            all << dep.module.id
            getManagedDependenciesRecursive(dep.children, all)
        }
        return all
    }
}
