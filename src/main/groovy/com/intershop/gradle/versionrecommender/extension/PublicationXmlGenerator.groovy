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
import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.publish.ivy.IvyPublication
import org.gradle.api.publish.maven.MavenPublication

class PublicationXmlGenerator {

    final static String EXTENSIONNAME = 'versionManagement'

    private Project project
    private RecommendationProviderContainer providerContainer

    PublicationXmlGenerator(Project project) {
        this.project = project
    }

    void withSubProjects(Closure projectClosure) {
        Iterable<Project> subprojects = null

        def projectsRet = projectClosure()

        if(projectsRet instanceof Project) {
            subprojects = [(Project) projectsRet]
        } else if(Iterable.class.isAssignableFrom(projectsRet.class)) {
            subprojects = projectsRet
        }

        generateDependencyXml(projectClosure.delegate.delegate, { subprojects.collect { ModuleNotationParser.parse(it) } }, project.versionRecommendation.provider)
    }

    void fromConfigurations(Closure configurationsClosure) {
        Iterable<Configuration> configurations

        def configurationsRet = configurationsClosure()

        if(configurationsRet instanceof Configuration) {
            configurations = [(Configuration) configurationsRet]
        }else if(Iterable.class.isAssignableFrom(configurationsRet.class)) {
            configurations = configurationsRet
        }

        generateDependencyXml(configurationsClosure.delegate.delegate, { configurations.collect { getManagedDependencies(it) }.flatten() }, project.versionRecommendation.provider)
    }

    void withDependencies(Closure dependenciesClosure) {
        Iterable<String> dependencies = null

        def dependenciesRet = dependenciesClosure()

        if(dependenciesRet instanceof String) {
            dependencies = [(String) dependenciesRet]
        } else if(Iterable.class.isAssignableFrom(dependenciesRet.class)) {
            dependencies = dependenciesRet
        }
        generateDependencyXml(dependenciesClosure.delegate.delegate, { dependencies.collect { ModuleNotationParser.parse(it) } }, project.versionRecommendation.provider)
    }

    protected static generateDependencyXml(IvyPublication pub, Closure<Iterable<ModuleVersionIdentifier>> deps, RecommendationProviderContainer rpc) {

        pub.descriptor.withXml { XmlProvider xml ->
            Node root = xml.asNode()

            root.dependencies[0].@defaultconfmapping = '*->default'

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
                new Node(root.dependencies[0], 'dependency', dependencyAttributes)
            }
        }
    }

    protected static generateDependencyXml(MavenPublication pub, Closure<Iterable<ModuleVersionIdentifier>> deps, RecommendationProviderContainer rpc) {
        pub.pom.withXml { XmlProvider xml ->
            Node root = xml.asNode()
            def dependencyManagement = root.getByName("dependencyManagement")

            // when merging two or more sources of dependencies, we want to only create one dependencyManagement section
            Node dependencies

            if(dependencyManagement.isEmpty()) {
                dependencies = root.appendNode("dependencyManagement").appendNode("dependencies")
            } else {
                dependencies = dependencyManagement[0].getByName("dependencies")[0]
            }

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

    protected static Set<ModuleVersionIdentifier> getManagedDependencies(Configuration configuration) {
        getManagedDependenciesRecursive(configuration.resolvedConfiguration.firstLevelModuleDependencies,
                new HashSet<ModuleVersionIdentifier>())
    }

    protected static Set<ModuleVersionIdentifier> getManagedDependenciesRecursive(Set<ResolvedDependency> deps, Set<ModuleVersionIdentifier> all) {
        deps.each {ResolvedDependency dep ->
            all << dep.module.id
            getManagedDependenciesRecursive(dep.children, all)
        }
        return all
    }
}
