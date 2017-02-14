package com.intershop.gradle.versionrecommender.util

import com.intershop.gradle.test.util.TestDir
import spock.lang.Specification

class SimpleVersionPropertiesSpec extends Specification {

    /**
     * Project directory for tests
     */
    @TestDir
    File testProjectDir

    def 'test read simple properties'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test.properties').getFile())
        File newFile = new File(testProjectDir, 'writetest.properties')
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new FileReader(file))

        then:
        svp.entrySet().size() == 3

        when:
        svp.store(newFile)

        then:
        file.text == newFile.text
    }

    def 'test read complex properties'() {
        when:
        ClassLoader classLoader = getClass().getClassLoader()
        File file = new File(classLoader.getResource('propertiestest/test2.properties').getFile())
        File newFile = new File(testProjectDir, 'writetest2.properties')
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new FileReader(file))

        then:
        svp.entrySet().size() == 3

        when:
        svp.store(newFile)

        then:
        file.text == newFile.text

        when:
        svp.setProperty('com.intershop.comment:commentprop', '11.12.20')
        File compareFile = new File(classLoader.getResource('propertiestest/test3.properties').getFile())
        svp.store(newFile)

        then:
        compareFile.text == newFile.text
    }

    def 'test with more spaces between key, equals and value'() {
        when:
        String testProps = """
        com.test:version1                  =            1.2.3
        com.test:version2=2.3.4
        # com.test:version3 = 1.0.0
        """.stripIndent()
        File newFile = new File(testProjectDir, 'writetest.properties')
        newFile.setText(testProps)
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new FileReader(newFile))

        then:
        svp.getProperty('com.test:version1') == '1.2.3'
        svp.getProperty('com.test:version2') == '2.3.4'
        svp.getProperty('com.test:version3') == null
    }
}
