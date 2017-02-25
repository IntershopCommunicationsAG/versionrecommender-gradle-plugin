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
package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.util.UpdatePos
import groovy.xml.MarkupBuilder
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Requires
import spock.lang.Specification

class VersionUpdaterSpec extends Specification {

    /**
     * Project directory for tests
     */
    @TestDir
    File testProjectDir

    /**
     * Test name
     */
    @Rule
    TestName testName = new TestName()

    /**
     * Canonical name of the test name
     */
    protected String canonicalName

    /**
     * Test project
     */
    protected Project project

    def setup() {
        canonicalName = testName.getMethodName().replaceAll(' ', '-')
        project = ProjectBuilder.builder().withName(canonicalName).withProjectDir(testProjectDir).build()
    }

    def 'getUpdateVersion from Maven with semantic versions from local directory'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.1') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.1'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '2.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '2.0.0'
            }
        }.writeTo(repoDir)

        File metadata = new File(repoDir, 'com/intershop/gradle/jaxb/jaxb-gradle-plugin/maven-metadata.xml')

        MarkupBuilder xmlMetadata = new MarkupBuilder(metadata.newWriter())
        xmlMetadata.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMetadata.metadata {
            groupId { mkp.yield('com.intershop.gradle.jaxb')}
            artifactId { mkp.yield('jaxb-gradle-plugin')}
            version { mkp.yield('2.0.0')}
            versioning {
                latest { mkp.yield('2.0.0')}
                release { mkp.yield('2.0.0')}
                versions {
                    version { mkp.yield('1.0.0')}
                    version { mkp.yield('1.0.1')}
                    version { mkp.yield('2.0.0')}
                }
                lastUpdated { mkp.yield('20160923190059')}
            }
        }

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin','1.0.0')

        then:
        metadata.exists()
        uv == '1.0.1'
    }

    def 'getUpdateVersion from Maven with semantic versions with different digits (apache) from local directory'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.2') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.2'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.1') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.1'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.2') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.2'
            }
        }.writeTo(repoDir)

        File metadata = new File(repoDir, 'com/intershop/gradle/jaxb/jaxb-gradle-plugin/maven-metadata.xml')

        MarkupBuilder xmlMetadata = new MarkupBuilder(metadata.newWriter())
        xmlMetadata.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMetadata.metadata {
            groupId { mkp.yield('com.intershop.gradle.jaxb')}
            artifactId { mkp.yield('jaxb-gradle-plugin')}
            version { mkp.yield('1.2')}
            versioning {
                latest { mkp.yield('1.2')}
                release { mkp.yield('1.2')}
                versions {
                    version { mkp.yield('1.0.2')}
                    version { mkp.yield('1.1')}
                    version { mkp.yield('1.2')}
                }
                lastUpdated { mkp.yield('20160923190059')}
            }
        }

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin','1.0.2', UpdatePos.MINOR)

        then:
        metadata.exists()
        uv == '1.2'
    }

    def 'getUpdateVersion from Maven with semantic versions and milestone builds from local directory'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.0-dev1') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0-dev1'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.0-dev2') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0-dev2'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.0-dev3') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0-dev3'
            }
        }.writeTo(repoDir)

        File metadata = new File(repoDir, 'com/intershop/gradle/jaxb/jaxb-gradle-plugin/maven-metadata.xml')

        MarkupBuilder xmlMetadata = new MarkupBuilder(metadata.newWriter())
        xmlMetadata.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMetadata.metadata {
            groupId { mkp.yield('com.intershop.gradle.jaxb')}
            artifactId { mkp.yield('jaxb-gradle-plugin')}
            version { mkp.yield('1.0.0-dev3')}
            versioning {
                latest { mkp.yield('1.0.0-dev3')}
                release { mkp.yield('1.0.0-dev3')}
                versions {
                    version { mkp.yield('1.0.0-dev1')}
                    version { mkp.yield('1.0.0-dev2')}
                    version { mkp.yield('1.0.0-dev3')}
                }
                lastUpdated { mkp.yield('20160923190059')}
            }
        }

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin','1.0.0-dev1')

        then:
        metadata.exists()
        uv == '1.0.0-dev3'
    }

    def 'getUpdateVersion from Maven with non semantic versions from local directory with different update configs'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.0.ga') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0.ga'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.0.1.ga') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.1.ga'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '1.1.0.ga') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.1.0.ga'
            }
            project(groupId: 'com.intershop.gradle.jaxb', artifactId:'jaxb-gradle-plugin', version: '2.0.0.ga') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '2.0.0.ga'
            }
        }.writeTo(repoDir)

        File metadata = new File(repoDir, 'com/intershop/gradle/jaxb/jaxb-gradle-plugin/maven-metadata.xml')

        MarkupBuilder xmlMetadata = new MarkupBuilder(metadata.newWriter())
        xmlMetadata.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMetadata.metadata {
            groupId { mkp.yield('com.intershop.gradle.jaxb')}
            artifactId { mkp.yield('jaxb-gradle-plugin')}
            version { mkp.yield('2.0.0.ga')}
            versioning {
                latest { mkp.yield('2.0.0.ga')}
                release { mkp.yield('2.0.0.ga')}
                versions {
                    version { mkp.yield('1.0.0.ga')}
                    version { mkp.yield('1.0.1.ga')}
                    version { mkp.yield('1.1.0.ga')}
                    version { mkp.yield('2.0.0.ga')}
                }
                lastUpdated { mkp.yield('20160923190059')}
            }
        }

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin','1.0.0.ga')

        then:
        metadata.exists()
        uv == null

        when:
        VersionUpdater vuwuc = new VersionUpdater(project)
        String uvwuc = vuwuc.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin', '1.0.0.ga', '\\.ga')

        then:
        uvwuc == '1.0.1.ga'

        when:
        VersionUpdater vudu = new VersionUpdater(project)
        String uvudu = vudu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin', '1.0.0.ga', '\\.ga', UpdatePos.MINOR)

        then:
        uvudu == '1.1.0.ga'
    }

    def 'getUpdateVersion from Maven with semantic versions from jcenter'() {
        when:
        project.repositories.add(project.repositories.jcenter())

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.jaxb','jaxb-gradle-plugin','1.0.0')

        then:
        uv == '1.0.1'
    }

    def 'getUpdateVersion from Maven with semantic versions from jcenter - module does not exists'() {
        when:
        project.repositories.add(project.repositories.jcenter())

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.doesnotexist','doesnotexist','1.0.0')

        then:
        uv == null
    }

    def 'getUpdateVersion from Maven with semantic versions - module exists in local'() {
        when:
        project.repositories.add(project.repositories.jcenter())

        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.gradle.existslocal', artifactId:'existslocal', version: '1.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0'
            }
            project(groupId: 'com.intershop.gradle.existslocal', artifactId:'existslocal', version: '1.0.1') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.1'
            }
            project(groupId: 'com.intershop.gradle.existslocal', artifactId:'existslocal', version: '2.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '2.0.0'
            }
        }.writeTo(repoDir)

        File metadata = new File(repoDir, 'com/intershop/gradle/existslocal/existslocal/maven-metadata.xml')

        MarkupBuilder xmlMetadata = new MarkupBuilder(metadata.newWriter())
        xmlMetadata.mkp.xmlDeclaration(version: "1.0", encoding: "utf-8")
        xmlMetadata.metadata {
            groupId { mkp.yield('com.intershop.gradle.jaxb')}
            artifactId { mkp.yield('jaxb-gradle-plugin')}
            version { mkp.yield('2.0.0')}
            versioning {
                latest { mkp.yield('2.0.0')}
                release { mkp.yield('2.0.0')}
                versions {
                    version { mkp.yield('1.0.0')}
                    version { mkp.yield('1.0.1')}
                    version { mkp.yield('2.0.0')}
                }
                lastUpdated { mkp.yield('20160923190059')}
            }
        }

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.intershop.gradle.existslocal','existslocal','1.0.0')

        then:
        uv == '1.0.1'
    }

    def 'getUpdateVersion from Ivy with semantic versions from local directory'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.1') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
        }.writeTo(repoDir)

        project.repositories {
            ivy {
                name 'ivyLocal'
                url "file://${repoDir.absolutePath}"
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)
        vu.ivyPattern = ivyPattern

        String uv = vu.getUpdateVersion('com.intershop.gradle.existslocal','existslocal','1.0.0')

        then:
        uv == '1.0.1'
    }

    def 'getUpdateVersion from Ivy with semantic versions from local directory - module does not exists'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.1') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
        }.writeTo(repoDir)

        project.repositories {
            ivy {
                name 'ivyLocal'
                url "file://${repoDir.absolutePath}"
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)
        vu.ivyPattern = ivyPattern

        String uv = vu.getUpdateVersion('com.intershop.gradle.doesnotexists','doesnotexists','1.0.0')

        then:
        uv == null
    }

    @Requires({
        System.properties['intershop.host.ivy.url'] &&
                System.properties['intershop.host.username'] &&
                System.properties['intershop.host.userpassword']
    })
    def 'getUpdateVersion from Ivy with semantic versions from repo'() {
        when:
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        project.repositories {
            ivy {
                name 'ivy'
                url System.properties['intershop.host.ivy.url']
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
                credentials {
                    username System.properties['intershop.host.username']
                    password System.properties['intershop.host.userpassword']
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)
        vu.ivyPattern = ivyPattern

        String uv = vu.getUpdateVersion('com.intershop.platform', 'core', '11.0.9')

        then:
        uv == '11.0.13'
    }

    @Requires({
        System.properties['intershop.host.mvn.url'] &&
                System.properties['intershop.host.username'] &&
                System.properties['intershop.host.userpassword']
    })
    def 'getUpdateVersion from MVN with semantic versions from repo'() {
        when:
        project.repositories {
            maven {
                name 'mvn'
                url System.properties['intershop.host.mvn.url']
                credentials {
                    username System.properties['intershop.host.username']
                    password System.properties['intershop.host.userpassword']
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)

        String uv = vu.getUpdateVersion('com.intershop.build.release', 'intershop-buildinfo-plugin', '2.0.1')

        then:
        uv == '2.0.4'
    }

    @Requires({
        System.properties['intershop.host.ivy.url'] &&
                System.properties['intershop.host.username'] &&
                System.properties['intershop.host.userpassword']
    })
    def 'getUpdateVersion from Ivy with semantic versions from repo - module does not exists'() {
        when:
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        project.repositories {
            ivy {
                name 'ivy'
                url System.properties['intershop.host.ivy.url']
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
                credentials {
                    username System.properties['intershop.host.username']
                    password System.properties['intershop.host.userpassword']
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)
        vu.ivyPattern = ivyPattern

        String uv = vu.getUpdateVersion('com.intershop.platform', 'doesnotexist', '11.0.9')

        then:
        uv == null
    }

    @Requires({
        System.properties['intershop.host.ivy.url'] &&
                System.properties['intershop.host.username'] &&
                System.properties['intershop.host.userpassword']
    })
    def 'getUpdateVersion from Ivy with semantic versions from repo - module exists only local'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '1.0.1') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
            module(org: 'com.intershop.gradle.existslocal', name:'existslocal', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
            }
        }.writeTo(repoDir)

        project.repositories {
            jcenter()
            ivy {
                name 'ivy'
                url System.properties['intershop.host.ivy.url']
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
                credentials {
                    username System.properties['intershop.host.username']
                    password System.properties['intershop.host.userpassword']
                }
            }
            ivy {
                name 'ivyLocal'
                url "file://${repoDir.absolutePath}"
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
            }
        }

        VersionUpdater vu = new VersionUpdater(project)
        vu.ivyPattern = ivyPattern

        String uv = vu.getUpdateVersion('com.intershop.gradle.existslocal', 'existslocal', '1.0.0')

        then:
        uv == '1.0.1'
    }

    def 'getUpdateVersion with jetty version from Maven with semantic versions from jcenter'() {
        when:
        project.repositories.add(project.repositories.jcenter())

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('org.eclipse.jetty','jetty-server','9.3.11.v20160721', '\\.v\\d+', UpdatePos.MINOR)

        then:
        uv != null
        uv.startsWith('9.4')
    }

    def 'getUpdateVersion with google version from Maven with semantic versions from jcenter'() {
        when:
        project.repositories.add(project.repositories.jcenter())

        VersionUpdater vu = new VersionUpdater(project)
        String uv = vu.getUpdateVersion('com.google.apis','google-api-services-appsactivity','v1-rev29-1.20.0', '^(v1-rev)(\\d+)(-1\\.20\\.0)$', 2)

        then:
        uv != 'v1-rev29-1.20.0'
    }
}
