package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.util.VersionExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class PropertiesProviderTest extends Specification {

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

    def 'Properties provider from file'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesProvider provider = new PropertiesProvider('test', project, file)

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
    }

    def 'Properties provider with properties only'() {
        when:
        PropertiesProvider provider = new PropertiesProvider('test', project, new File('empty.properties'))

        provider.setVersionList(['com.intershop.platform:platformcomp':'1.2.3',
                                'com.intershop.business:businesscomp':'2.4.5'])

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
    }

    def 'Properties provider with properties and file'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesProvider provider = new PropertiesProvider('test', project, file)

        provider.setVersionList(['com.intershop.content:contentcomp':'1.1.1',
                                 'com.intershop.test:testcomp':'1.1.2'])

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
        provider.getVersion('com.intershop.test','testcomp') == '1.1.2'
    }

    def 'Properties provider from file with placeholders'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesProvider provider = new PropertiesProvider('test', project, file)

        provider.setVersionList(['com.intershop.content:*':'1.1.1',
                                 'com.intershop.test:testcomp':'1.1.2'])

        then:
        provider.getVersion('com.intershop.content','testcompa') == '1.1.1'
        provider.getVersion('com.intershop.content','testcompb') == '1.1.1'
    }

    def 'Properties provider from file and properties with dependencies'() {
        when:
        PropertiesProvider provider = new PropertiesProvider('test', project, new File('empty.properties'))

        provider.setVersionList(['org.hibernate:hibernate-validator':'5.3.0.Final',
                                 'org.tmatesoft.svnkit:svnkit':'1.8.14'])

        provider.setTransitive(true)

        then:
        provider.getVersion('javax.validation','validation-api') == '1.1.0.Final'
        provider.getVersion('org.tmatesoft.svnkit','svnkit') == '1.8.14'
    }

    def 'Properties provider from file with LOCAL extension'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test3.properties').getFile())

        File confFile = new File(project.projectDir, 'version.properties')
        confFile << file.getText()

        PropertiesProvider provider = new PropertiesProvider('test', project, confFile)
        provider.setVersionExtension(VersionExtension.LOCAL)

        then:
        provider.getVersion('com.intershop.platform', 'platformcomp') == '1.2.3-LOCAL'
    }
}
