package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.util.VersionExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class MavenProviderSpec extends Specification {

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

    def 'Maven provider with bom file'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('mvntest/hibernate-validator-5.3.0.Final.pom').getFile())

        MavenProvider provider = new MavenProvider('test', project, file)

        then:
        provider.getVersion('javax.validation','validation-api') == '1.1.0.Final'
    }

    def 'Maven provider with dependency configuration'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId:'filter', version: '2.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0'
                dependency groupId: 'com.intershop', artifactId: 'component2', version: '2.0.0'
            }
        }.writeTo(repoDir)

        project.repositories.maven {
            name 'mvnLocal'
            url "file://${repoDir.absolutePath}"
        }

        MavenProvider provider = new MavenProvider('test', project, 'com.intershop:filter:2.0.0')

        then:
        provider.getVersion('com.intershop', 'component1') == '1.0.0'
    }

    def 'Maven provider with local dependency configuration'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')
        File localRepoDir = new File(testProjectDir, 'repo')

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId:'filter', version: '2.0.0') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0'
                dependency groupId: 'com.intershop', artifactId: 'component2', version: '2.0.0'
            }
        }.writeTo(repoDir)

        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId:'filter', version: '2.0.0-LOCAL') {
                dependency groupId: 'com.intershop', artifactId: 'component1', version: '1.0.0'
                dependency groupId: 'com.intershop', artifactId: 'component2', version: '2.0.0-LOCAL'
            }
        }.writeTo(localRepoDir)

        project.repositories {
            maven {
                name 'mvnLocal'
                url "file://${repoDir.absolutePath}"
            }
            maven {
                name 'mvnLocalLocal'
                url "file://${localRepoDir.absolutePath}"
            }
        }

        MavenProvider provider = new MavenProvider('test', project, 'com.intershop:filter:2.0.0')
        provider.setVersionExtension(VersionExtension.LOCAL)

        then:
        provider.getVersion('com.intershop', 'component1') == '1.0.0'
        provider.getVersion('com.intershop', 'component2') == '2.0.0-LOCAL'

        when:
        provider.setVersionExtension(VersionExtension.NONE)

        then:
        provider.getVersion('com.intershop', 'component1') == '1.0.0'
        provider.getVersion('com.intershop', 'component2') == '2.0.0'
    }
}
