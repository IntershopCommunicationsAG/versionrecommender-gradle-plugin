package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.util.VersionExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class IvyProviderSpec extends Specification {

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

    def 'Ivy provider spec'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('ivytest/ivy.xml').getFile())

        IvyProvider provider = new IvyProvider('test', project, file)

        then:
        provider.getVersion('javax.inject','javax.inject') == '1'
    }

    def 'Ivy provider with dependencies'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('ivytest/ivy.xml').getFile())

        IvyProvider provider = new IvyProvider('test', project, file)
        provider.transitive = true

        then:
        provider.getVersion('aopalliance', 'aopalliance') == '1.0'
    }

    def 'Ivy provider with dependency configuration'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '2.0.0'
            }
        }.writeTo(repoDir)

        project.repositories.ivy {
            name 'ivyLocal'
            url "file://${repoDir.absolutePath}"
            layout ('pattern') {
                ivy ivyPattern
                artifact artifactPattern
                artifact ivyPattern
            }
        }

        IvyProvider provider = new IvyProvider('test', project, 'com.intershop:filter:2.0.0')

        then:
        provider.getVersion('com.intershop', 'component1') == '1.0.0'
    }

    def 'Ivy provider with local dependency configuration'() {
        when:
        File repoDir = new File(testProjectDir, 'repo')
        File localRepoDir = new File(testProjectDir, 'localrepo')
        String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
        String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '2.0.0'
            }
        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '2.0.0-LOCAL') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '2.0.0-LOCAL'
            }
        }.writeTo(localRepoDir)

        project.repositories {
            ivy {
                name 'ivyLocal'
                url "file://${repoDir.absolutePath}"
                layout ('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
            }
            ivy {
                name 'ivyLocalLocal'
                url "file://${localRepoDir.absolutePath}"
                layout('pattern') {
                    ivy ivyPattern
                    artifact artifactPattern
                    artifact ivyPattern
                }
            }
        }

        IvyProvider provider = new IvyProvider('test', project, 'com.intershop:filter:2.0.0')
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
