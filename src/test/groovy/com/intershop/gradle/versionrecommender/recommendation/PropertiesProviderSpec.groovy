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
package com.intershop.gradle.versionrecommender.recommendation

import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.update.UpdateConfigurationItem
import com.intershop.gradle.versionrecommender.util.VersionExtension
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class PropertiesProviderSpec extends Specification {

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
        project.repositories.add(project.repositories.jcenter())
    }

    def 'Properties provider from file'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, file)

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
    }

    def 'Properties provider with properties only'() {
        when:
        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, new File('empty.properties'))

        provider.setVersionMap(['com.intershop.platform:platformcomp':'1.2.3',
                                'com.intershop.business:businesscomp':'2.4.5'])

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
    }

    def 'Properties provider with properties and file'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, file)

        provider.setVersionMap(['com.intershop.content:contentcomp':'1.1.1',
                                 'com.intershop.test:testcomp':'1.1.2'])

        then:
        provider.getVersion('com.intershop.business','businesscomp') == '2.4.5'
        provider.getVersion('com.intershop.test','testcomp') == '1.1.2'
    }

    def 'Properties provider from file with placeholders'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())

        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, file)

        provider.setVersionMap(['com.intershop.content:*':'1.1.1',
                                 'com.intershop.test:testcomp':'1.1.2'])

        then:
        provider.getVersion('com.intershop.content','testcompa') == '1.1.1'
        provider.getVersion('com.intershop.content','testcompb') == '1.1.1'
    }

    def 'Properties provider from file and properties with dependencies'() {
        when:
        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, new File('empty.properties'))

        provider.setVersionMap(['org.hibernate:hibernate-validator':'5.3.0.Final',
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

        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, confFile)
        provider.setVersionExtension(VersionExtension.LOCAL)

        then:
        provider.getVersion('com.intershop.platform', 'platformcomp') == '1.2.3-LOCAL'
    }

    def 'Properties provider version update'() {
        when:
        String propertiesContent = """
        # external properties
        org.apache.commons:commons-lang3 = 3.3
        org.eclipse.jetty:jetty-server = 9.3.11.v20160721
        org.apache.logging.log4j:log4j-core = 2.4
        """.stripIndent()

        File confFile = new File(project.projectDir, 'version.properties')
        confFile.setText(propertiesContent)

        PropertiesRecommendationProvider provider = new PropertiesRecommendationProvider('test', project, confFile)
        project.repositories.add(project.repositories.jcenter())

        then:
        provider.getVersion('org.apache.commons', 'commons-lang3').startsWith('3.3')
        provider.getVersion('org.eclipse.jetty', 'jetty-server') == '9.3.11.v20160721'
        provider.getVersion('org.apache.logging.log4j', 'log4j-core') == '2.4'

        when:
        UpdateConfiguration uc = new UpdateConfiguration(project)

        UpdateConfigurationItem uci_1 = new UpdateConfigurationItem('jetty', 'org.eclipse.jetty', '')
        uci_1.searchPattern = '\\.v\\d+'
        uc.addConfigurationItem(uci_1)
        UpdateConfigurationItem uci_2 = new UpdateConfigurationItem('default')
        uc.addConfigurationItem(uci_2)

        provider.update(uc)

        then:
        provider.getVersion('org.apache.commons', 'commons-lang3') == '3.3.2'
        provider.getVersion('org.eclipse.jetty', 'jetty-server') == '9.3.16.v20170120'
        provider.getVersion('org.apache.logging.log4j', 'log4j-core') == '2.4.1'

        when:
        provider.store()

        then:
        confFile.text.contains('org.apache.commons:commons-lang3 = 3.3.2')
    }
}
