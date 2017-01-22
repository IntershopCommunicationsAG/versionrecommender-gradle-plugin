package com.intershop.gradle.versionrecommender

import com.intershop.gradle.test.AbstractIntegrationSpec


class IntVersionRecommenderPluginSpec extends AbstractIntegrationSpec {

    def 'test simple configuration'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommender {
                providers {
                    ivy('test1) {
                        org = 'test.org'
                        module = 'test'
                        version = '1.0.0'
                    }
                }
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()
        writeJavaTestClass("com.intershop.test")

        when:
        def result = getPreparedGradleRunner()
                .withArguments('tasks')
                .build()

        then:
        println result.output

    }
}
