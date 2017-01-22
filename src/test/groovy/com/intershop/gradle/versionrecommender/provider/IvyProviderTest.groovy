package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class IvyProviderTest extends Specification {

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

    def 'Check IVY Provider'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('ivytest/ivy.xml').getFile())

        IvyProvider provider = new IvyProvider('test', project, file)

        then:
        provider.getVersion('javax.inject','javax.inject') == '1'
    }

    def 'Check IVY Provider - with dependencies'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('ivytest/ivy.xml').getFile())

        IvyProvider provider = new IvyProvider('test', project, file)
        provider.transitive = true

        then:
        provider.getVersion('aopalliance', 'aopalliance') == '1.0'
    }
}
