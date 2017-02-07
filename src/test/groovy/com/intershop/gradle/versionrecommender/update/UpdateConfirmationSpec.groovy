package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.test.util.TestDir
import com.intershop.gradle.versionrecommender.util.UpdatePos
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class UpdateConfirmationSpec extends Specification {

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

    def 'return configuration list of configItems'() {
        when:
        UpdateConfiguration uc = new UpdateConfiguration(project)

        UpdateConfigurationItem uci1 = new UpdateConfigurationItem('filter', 'com.intershop.*', '')
        uci1.updatePos = UpdatePos.MINOR
        uc.addConfigurationItem(uci1)

        UpdateConfigurationItem uci2 = new UpdateConfigurationItem('core', 'com.intershop.platform', 'core')
        uci2.updatePos = UpdatePos.MAJOR
        uc.addConfigurationItem(uci2)

        UpdateConfigurationItem uci3 = new UpdateConfigurationItem('runtime', 'com.intershop.platform', 'runtime')
        uci3.updatePos = UpdatePos.MINOR
        uc.addConfigurationItem(uci3)

        UpdateConfigurationItem uci4 = new UpdateConfigurationItem('platform', 'com.intershop.platform', '')
        uci4.updatePos = UpdatePos.HOTFIX
        uc.addConfigurationItem(uci4)

        UpdateConfigurationItem uci5 = new UpdateConfigurationItem('jetty', 'org.jetty', '')
        uci5.searchPattern = '\\.v\\d+'
        uc.addConfigurationItem(uci5)

        UpdateConfigurationItem ucif1 = uc.getConfigItem('com.intershop.business', 'ac_adapter')
        UpdateConfigurationItem ucif2 = uc.getConfigItem('org.jetty', 'server')
        UpdateConfigurationItem ucif3 = uc.getConfigItem('com.intershop.platform', 'runtime')
        UpdateConfigurationItem ucif4 = uc.getConfigItem('com.intershop.platform', 'core')
        UpdateConfigurationItem ucif5 = uc.getConfigItem('com.intershop.platform', 'test')
        UpdateConfigurationItem ucif6 = uc.getConfigItem('com.intershop.business', 'core')
        UpdateConfigurationItem ucif7 = uc.getConfigItem('com.intershop.content', 'contentcomp')
        UpdateConfigurationItem ucif8 = uc.getConfigItem('org.apache', 'server')

        then:
        ucif1.updatePos == UpdatePos.MINOR
        ucif6.updatePos == UpdatePos.MINOR
        ucif7.updatePos == UpdatePos.MINOR
        ucif2.updatePos == UpdatePos.HOTFIX
        ucif5.updatePos == UpdatePos.HOTFIX
        ucif3.updatePos == UpdatePos.MINOR
        ucif4.updatePos == UpdatePos.MAJOR
        ucif8.updatePos == UpdatePos.HOTFIX
    }
}
