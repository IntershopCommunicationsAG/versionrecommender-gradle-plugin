package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class MavenProviderTest extends Specification {

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

    def 'Test Mvn Provider - pom only'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('mvntest/hibernate-validator-5.3.0.Final.pom').getFile())

        MavenProvider provider = new MavenProvider('test', project, file)

        then:
        provider.getVersion('javax.validation','validation-api') == '1.1.0.Final'
    }
}
