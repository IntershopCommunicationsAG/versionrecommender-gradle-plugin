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
package com.intershop.gradle.versionrecommender

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import static org.gradle.testkit.runner.TaskOutcome.*

class IntVersionRecommenderPluginSpec extends AbstractIntegrationSpec {

    final static String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
    final static String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'


    def 'test simple configuration with ivy'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    ivy('filter',  'com.intershop:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
                testConfig 'org.apache.tomcat:tomcat-catalina:8.5.5'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'result/tomcat-catalina-8.5.5.jar')).exists()

        when:
        def resultTasks = getPreparedGradleRunner()
                .withArguments('tasks', '-s') //, '--profile')
                .build()

        then:
        resultTasks.output.contains('setLocalFilter')
        resultTasks.output.contains('setSnapshotFilter')
        resultTasks.output.contains('resetFilter')
        resultTasks.output.contains('updateFilter')

        when:
        def resultSetLocal = getPreparedGradleRunner()
                .withArguments('setLocalFilter', '-s') //, '--profile')
                .build()

        then:
        resultSetLocal.task(':setLocalFilter').outcome == SUCCESS

        when:
        def resultAfterSetLocal = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0-LOCAL.xml')).exists()

        when:
        def resultReset = getPreparedGradleRunner()
                .withArguments('resetFilter', '-s') //, '--profile')
                .build()

        then:
        resultReset.task(':resetFilter').outcome == SUCCESS

        when:
        def resultAfterReset = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('updateFilter', '-s') //, '--profile')
                .build()

        then:
        resultUpdate.task(':updateFilter').outcome == SUCCESS

        when:
        def resultAfterUpdate = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.1.xml')).exists()
    }

    def 'test with force recommendation version'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                provider {
                    ivy('filter',  'com.intershop:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
                testConfig 'org.apache.tomcat:tomcat-catalina:8.5.5@ivy'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-9.1.0.xml')).exists()
    }

    def 'test with two filters'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                provider {
                    ivy('filter1',  'com.intershop:filter:1.0.0') {}
                    ivy('filter2',  'com.intershop:altfilter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
                testConfig 'com.intershop:component3@ivy'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-3.2.0.xml')).exists()
    }

    def 'test with two filters different order'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                provider {
                    ivy('filter2',  'com.intershop:altfilter:1.0.0') {}
                    ivy('filter1',  'com.intershop:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
                testConfig 'com.intershop:component3@ivy'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-3.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-3.2.0.xml')).exists()
    }

    def 'test with two filters, one without unspecified version'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                provider {
                    ivy('filter2',  'com.intershop:altfilter') {}
                    ivy('filter1',  'com.intershop:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()
    }

    def 'test simple configuration with properties and without files'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    properties('list') {
                        versionMap = [
                            'com.intershop.test:testcomp1': '10.0.0',
                            'com.intershop.testglob:*': '11.0.0',
                        ]
                    }
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop.test:testcomp1@ivy'
                testConfig 'com.intershop.testglob:testglob1@ivy'
                testConfig 'com.intershop.testglob:testglob10@ivy'
            }
            
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }

            ${writeIvyRepo(testProjectDir)}
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        (new File(testProjectDir, 'result/ivy-10.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-11.0.0.xml')).exists()
    }

    def 'test complex properties update'() {
        given:
        copyResources('updatetest/test.version', 'properties.version')

        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    properties('complex', project.file('properties.version')) {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            module = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.google.inject:guice'
            }
                     
            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('updateComplex', '-s') //, '--profile')
                .build()

        then:
        resultUpdate.task(':updateComplex').outcome == SUCCESS
    }

    def 'test complex properties update with exclude configuration'() {
        given:
        copyResources('updatetest/test.version', 'properties.version')

        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    properties('complex', project.file('properties.version')) {
                        changeExcludes = ['org.jboss.logging:*']
                    }
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItemContainer {
                        testUpdate1 {
                            org = 'org.eclipse.jetty'
                            searchPattern = '\\\\.v\\\\d+'
                        }
                    }
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.google.inject:guice'
            }
                     
            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('updateComplex', '-s') //, '--profile')
                .build()

        then:
        resultUpdate.task(':updateComplex').outcome == SUCCESS
        ! resultUpdate.output.contains('org.jboss.logging')
        ! resultUpdate.output.contains('No changes on properties')

    }

    def 'test update with a multi provider configuration'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                
                provider {
                    ivy('filter5',  'com.intershop:altfilter') {}
                    ivy('filter4',  'com.intershop:filter:1.0.0') {}
                    ivy('filter3',  'com.intershop.other:filter:1.0.0') {}
                    ivy('filter2',  'com.intershop.another:filter:1.0.0') {}
                    ivy('filter1',  'com.intershop.woupdate:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    defaultUpdateProvider = ['filter4','filter3','filter2']
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
            }
                     
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }
            
            ${writeIvyRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('update', '-s') //, '--profile')
                .build()

        File fileFilter5 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter5.version')
        File fileFilter4 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter4.version')
        File fileFilter3 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter3.version')
        File fileFilter2 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter2.version')
        File fileFilter1 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter1.version')

        then:
        resultUpdate.task(':update').outcome == SUCCESS
        ! fileFilter5.exists()
        fileFilter4.exists()
        fileFilter3.exists()
        fileFilter2.exists()
        ! fileFilter1.exists()
        fileFilter4.text == '1.0.1'
        fileFilter3.text == '1.0.1'
        fileFilter2.text == '1.0.1'

        when:
        def resultStore = getPreparedGradleRunner()
                .withArguments('store', '-s') //, '--profile')
                .build()

        File storeFileFilter5 = new File(testProjectDir, '.ivyFilter5.version')
        File storeFileFilter4 = new File(testProjectDir, '.ivyFilter4.version')
        File storeFileFilter3 = new File(testProjectDir, '.ivyFilter3.version')
        File storeFileFilter2 = new File(testProjectDir, '.ivyFilter2.version')
        File storeFileFilter1 = new File(testProjectDir, '.ivyFilter1.version')

        then:
        resultStore.task(':store').outcome == SUCCESS
        ! storeFileFilter5.exists()
        storeFileFilter4.exists()
        storeFileFilter3.exists()
        storeFileFilter2.exists()
        ! storeFileFilter1.exists()
        storeFileFilter4.text == '1.0.1'
        storeFileFilter3.text == '1.0.1'
        storeFileFilter2.text == '1.0.1'

        when:
        def resultAltSet = getPreparedGradleRunner()
                .withArguments('setLocalFilter5', '-s') //, '--profile')
                .buildAndFail()

        then:
        resultAltSet.task(':setLocalFilter5').outcome == FAILED

        when:
        def resultAltSet2 = getPreparedGradleRunner()
                .withArguments('setLocalFilter5', '-Pfilter5Version=1.0.0', '-s') //, '--profile')
                .build()
        File updateFileFilter5 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter5.version')

        then:
        resultAltSet2.task(':setLocalFilter5').outcome == SUCCESS
        updateFileFilter5.exists()
        updateFileFilter5.text == '1.0.0-LOCAL'

        when:
        def resultResetAll= getPreparedGradleRunner()
                .withArguments('reset', '-s') //, '--profile')
                .build()
        File recFilter = new File(testProjectDir, 'build/versionRecommendation')

        then:
        resultResetAll.task(':reset').outcome == SUCCESS
        recFilter.listFiles().size() == 0

        when:
        def resultSet = getPreparedGradleRunner()
                .withArguments('setFilter5', '-Pfilter5Version=1.0.0', '-s') //, '--profile')
                .build()

        File updateFileFilter5set = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter5.version')

        then:
        resultSet.task(':setFilter5').outcome == SUCCESS
        updateFileFilter5set.exists()
        updateFileFilter5set.text == '1.0.0'

        when:
        def resultAlt = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()
        File copyResult = new File(testProjectDir, 'result/ivy-3.0.0.xml')

        then:
        resultAlt.task(':copyResult').outcome == SUCCESS
        copyResult.exists()
    }

    def 'test update and store with a multi provider configuration'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                
                provider {
                    ivy('filter5',  'com.intershop:altfilter') {}
                    ivy('filter4',  'com.intershop:filter:1.0.0') {}
                    ivy('filter3',  'com.intershop.other:filter:1.0.0') {}
                    ivy('filter2',  'com.intershop.another:filter:1.0.0') {}
                    ivy('filter1',  'com.intershop.woupdate:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    defaultUpdateProvider = ['filter4','filter3','filter2']
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
            }
                     
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }
            
            ${writeIvyRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultStore = getPreparedGradleRunner()
                .withArguments('store', 'update', '-s') //, '--profile')
                .build()

        File storeFileFilter5 = new File(testProjectDir, '.ivyFilter5.version')
        File storeFileFilter4 = new File(testProjectDir, '.ivyFilter4.version')
        File storeFileFilter3 = new File(testProjectDir, '.ivyFilter3.version')
        File storeFileFilter2 = new File(testProjectDir, '.ivyFilter2.version')
        File storeFileFilter1 = new File(testProjectDir, '.ivyFilter1.version')

        then:
        resultStore.task(':store').outcome == SUCCESS
        resultStore.task(':update').outcome == SUCCESS
        !storeFileFilter5.exists()
        storeFileFilter4.exists()
        storeFileFilter3.exists()
        storeFileFilter2.exists()
        !storeFileFilter1.exists()
        storeFileFilter4.text == '1.0.1'
        storeFileFilter3.text == '1.0.1'
        storeFileFilter2.text == '1.0.1'
    }


    def 'test update with a multi provider configuration and with different update configuration'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                
                provider {
                    ivy('filter5',  'com.intershop:altfilter') {}
                    ivy('filter4',  'com.intershop:filter:1.0.0') { }
                    ivy('filter3',  'com.intershop.other:filter:1.0.0') {
                        transitive = true
                        overrideTransitiveDeps = true
                    }
                    ivy('filter2',  'com.intershop.another:filter:1.0.0') {
                        configDir = file('filter2')
                    }
                    ivy('filter1',  'com.intershop.woupdate:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    defaultUpdateProvider = ['filter4','filter3','filter2']
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop:component1@ivy'
            }
                     
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }
            
            ${writeIvyRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('update', '-s') //, '--profile')
                .build()

        File fileFilter5 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter5.version')
        File fileFilter4 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter4.version')
        File fileFilter3 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter3.version')
        File fileFilter2 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter2.version')
        File fileFilter1 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter1.version')

        then:
        resultUpdate.task(':update').outcome == SUCCESS
        !fileFilter5.exists()
        fileFilter4.exists()
        fileFilter3.exists()
        fileFilter2.exists()
        !fileFilter1.exists()
        fileFilter4.text == '1.0.1'
        fileFilter3.text == '1.0.1'
        fileFilter2.text == '1.0.1'

        when:
        def resultStore = getPreparedGradleRunner()
                .withArguments('store', '-s') //, '--profile')
                .build()

        File fileStoreFilter2 = new File(testProjectDir, 'filter2/.ivyFilter2.version')

        then:
        resultStore.task(':store').outcome == SUCCESS
        fileStoreFilter2.exists()
        fileStoreFilter2.text == '1.0.1'
    }

    def 'test override with a multi provider configuration and with different configuration'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                forceRecommenderVersion = true
                
                provider {
                    ivy('filter5',  'com.intershop:altfilter') {}
                    ivy('filter4',  'com.intershop:filter:1.0.0') { }
                    ivy('filter3',  'com.intershop.other:filter:1.0.0') {
                        transitive = true
                        overrideTransitiveDeps = true
                    }
                    ivy('filter2',  'com.intershop.another:filter:1.0.0') {
                        configDir = file('filter2')
                    }
                    ivy('filter1',  'com.intershop.woupdate:filter:1.0.0') {}
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    defaultUpdateProvider = ['filter4','filter3','filter2']
                }
            }
            
            configurations {
                create('testConfig')
            }
        
            dependencies {
                testConfig 'com.intershop.other:component1@ivy'
                testConfig 'com.intershop.other:component2@ivy'
                testConfig 'com.intershop.other:depcomponent1@ivy'
            }
                     
            task copyResult(type: Copy) {
                into new File(projectDir, 'result')
                from configurations.testConfig
            }
            
            ${writeIvyRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('copyResult', '-s') //, '--profile')
                .build()

        then:
        resultUpdate.task(':copyResult').outcome == SUCCESS
        (new File(testProjectDir, 'result/ivy-1.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-2.0.0.xml')).exists()
        (new File(testProjectDir, 'result/ivy-10.0.0.xml')).exists()
    }

    def 'test simple configuration for multiproject'() {
        given:

        String repo = writeIvyRepo(testProjectDir)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.versionrecommender'
        }
            
        group = 'com.intershop'
        version = '1.0.0'
        
        versionRecommendation {
            provider {
                ivy('filter4',  'com.intershop:filter:2.0.0') {}
            }
        }
        
        configurations {
            create('testConfig')
        }
        
        dependencies {
            testConfig 'com.intershop:component1@ivy'
        }
                 
        task copyResult(type: Copy) {
            into new File(projectDir, 'result')
            from configurations.testConfig
        }

        ${repo}

        repositories {
            jcenter()
        }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'p_testProject'
        """.stripIndent()

        String subBuildFile1 = """
        apply plugin: 'com.intershop.gradle.versionrecommender'
        
        versionRecommendation {
            provider {
                ivy('filter3',  'com.intershop.other:filter:1.0.0') {}
            }
        }
            
        configurations {
            create('testConfig1')
        }
        
        task copyResult(type: Copy) {
            into new File(projectDir, 'result')
            from configurations.testConfig1
        }
            
        dependencies {
            testConfig1 'com.intershop.other:component1@ivy'
        }

        ${repo}
        """

        String subBuildFile2 = """
        apply plugin: 'com.intershop.gradle.versionrecommender'
        
        versionRecommendation {
            provider {
                ivy('filter2',  'com.intershop.another:filter:1.0.0') {}
            }
            updateConfiguration {
                ivyPattern = '${ivyPattern}'

                updateConfigItemContainer {
                    testUpdate1 {
                        org = 'com.intershop.another'
                        update = 'MINOR'
                    }
                }                    
            }
        }
            
        configurations {
            create('testConfig2')
        }
        
        task copyResult(type: Copy) {
            into new File(projectDir, 'result')
            from configurations.testConfig2
        }
        
        dependencies {
            testConfig2 'com.intershop.another:component1@ivy'
            testConfig2 'com.intershop:component2@ivy'
        }
        
        ${repo}
        """

        createSubProject('project1a', settingsfile, subBuildFile1)
        createSubProject('project2b', settingsfile, subBuildFile2)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('copyResult', '-s', '-i') //, '--profile')
                .build()

        then:
        result.task(':copyResult').outcome == SUCCESS
        result.task(':project1a:copyResult').outcome == SUCCESS
        result.task(':project2b:copyResult').outcome == SUCCESS

        when:
        def resultTasks = getPreparedGradleRunner()
                .withArguments('tasks', '--all', '-i') //, '--profile')
                .build()

        then:
        resultTasks.output.contains('resetFilter')
        !resultTasks.output.contains('project2b:resetFilter')

        when:
        def resultUpdate = getPreparedGradleRunner()
                .withArguments('updateFilter2', '-i') //, '--profile')
                .build()

        File fileFilter2 = new File(testProjectDir, 'build/versionRecommendation/.ivyFilter2.version')

        then:
        resultUpdate.task(':updateFilter2').outcome == SUCCESS
        fileFilter2.exists()
        fileFilter2.text == '1.1.0'
    }

    def 'test publishing with ivy'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
                id 'java'
                id 'ivy-publish'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    ivy('filter',  'com.intershop:filter:1.0.0') {}
                }
            }

            publishing {
                publications {
                    ivyJava(IvyPublication) {
                        from components.java
                    }
                }
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\$buildDir/repo"
                        layout('pattern') {
                            ivy "${ivyPattern}"
                            artifact "${artifactPattern}"
                        }
                    }
                }
            }
                        
            dependencies {
                compile 'org.apache.logging.log4j:log4j-core'
                compile 'commons-io:commons-io'
                runtime 'commons-codec:commons-codec'
            }

            ${writeIvyReadRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File javaFile = file('src/main/java/com/intershop/log4jExample.java', testProjectDir)
        javaFile << """package com.intershop;

            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            import org.apache.commons.io.FileUtils;
            import java.util.List;
            import java.io.File;
            import java.io.IOException;
            
            public class log4jExample{
            
               /* Get actual class name to be printed on */
               static Logger log = LogManager.getLogger(log4jExample.class.getName());
               
               public static void main(String[] args) throws IOException {
                  log.debug("Hello this is a debug message");
                  log.info("Hello this is an info message");
                  
                  File file = new File("/commons/io/project.properties");
                  List lines = FileUtils.readLines(file, "UTF-8");
               }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-i') //, '--profile')
                .build()
        File ivyFile = new File(testProjectDir, 'build/repo/com.intershop/testProject/1.0.0/ivys/ivy-1.0.0.xml')

        then:
        result.task(':publish').outcome == SUCCESS
        ivyFile.exists()
        ivyFile.text.contains('<dependency org="commons-codec" name="commons-codec" conf="runtime-&gt;default" rev="1.4"/>')
    }

    def 'test publishing with maven'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
                id 'java'
                id 'maven-publish'
            }
            
            group = 'com.intershop'
            version = '1.0.0'
            
            versionRecommendation {
                provider {
                    ivy('filter',  'com.intershop:filter:1.0.0') {}
                }
            }

            publishing {
                publications {
                    mavenJava(MavenPublication) {
                        from components.java
                    }
                }
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\$buildDir/repo"
                    }
                }
            }
                        
            dependencies {
                compile 'org.apache.logging.log4j:log4j-core'
                compile 'commons-io:commons-io'
                runtime 'commons-codec:commons-codec'
            }

            ${writeIvyReadRepo(testProjectDir)}

            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File javaFile = file('src/main/java/com/intershop/log4jExample.java', testProjectDir)
        javaFile << """package com.intershop;

            import org.apache.logging.log4j.LogManager;
            import org.apache.logging.log4j.Logger;
            import org.apache.commons.io.FileUtils;
            import java.util.List;
            import java.io.File;
            import java.io.IOException;
            
            public class log4jExample{
            
               /* Get actual class name to be printed on */
               static Logger log = LogManager.getLogger(log4jExample.class.getName());
               
               public static void main(String[] args) throws IOException {
                  log.debug("Hello this is a debug message");
                  log.info("Hello this is an info message");
                  
                  File file = new File("/commons/io/project.properties");
                  List lines = FileUtils.readLines(file, "UTF-8");
               }
            }
        """.stripIndent()

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-s') //, '--profile')
                .build()
        File ivyFile = new File(testProjectDir, 'build/repo/com.intershop/testProject/1.0.0/ivys/ivy-1.0.0.xml')

        then:
        result.task(':publish').outcome == SUCCESS
    }

    def 'test publishing ivy filter with projects and specified versions'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
            }

            apply plugin: 'java'
            apply plugin: 'ivy-publish'
                
            group = 'com.intershop'
            version = '1.0.0'

            dependencies {
                compile 'commons-configuration:commons-configuration:1.6' 
            }

            subprojects {
                group = 'com.intershop'
                version = '1.0.0'                
            }

            publishing {
                publications {
                    ivyFilter(IvyPublication) {
                        module 'ivy-filter'
                        revision project.version

                        // adds all sub projects
                        versionManagement.withSubProjects { subprojects }   

                        // the transitive closure of this configuration will be flattened 
                        // and added to the dependency management section
                        versionManagement.fromConfigurations { project.configurations.compile }  

                        // alternative syntax when you want to explicitly add a dependency with no transitives
                        versionManagement.withDependencies { 'manual:dep:1' }

                        // further customization of the POM is allowed if desired
                        descriptor.withXml { asNode().info[0].appendNode('description', 'A demonstration of IVY customization') }
                    }
                }
                repositories {
                    ivy {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\$buildDir/repo"
                        layout('pattern') {
                            ivy "${ivyPattern}"
                            artifact "${artifactPattern}"
                        }
                    }
                }                
            }
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File proj1Dir = createSubProject('project1a', settingsfile, '')
        File proj2Dir = createSubProject('project2b', settingsfile, '')

        writeJavaTestClass('com.intershop.project1', proj1Dir)
        writeJavaTestClass('com.intershop.project2', proj2Dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .build()

        File ivy = new File(testProjectDir, 'build/repo/com.intershop/ivy-filter/1.0.0/ivys/ivy-1.0.0.xml')

        then:
        result.task(':publish').outcome == SUCCESS
        ivy.text.contains('<dependency org="com.intershop" name="project1a" rev="1.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="com.intershop" name="project2b" rev="1.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="manual" name="dep" rev="1" conf="default"/>')
    }

    def 'test publishing maven filter with projects and specified versions'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
                id 'maven-publish'
            }
            
            apply plugin: 'java'
            apply plugin: 'maven-publish'
                
            group = 'com.intershop'
            version = '1.0.0'

            subprojects {
                group = 'com.intershop'
                version = '1.0.0'                
            }

            dependencies {
                compile 'commons-configuration:commons-configuration:1.6'
            }

            publishing {
                publications {
                    mvnFilter(MavenPublication) {
                        artifactId 'mvn-filter'
                        version project.version

                        // adds all sub projects
                        versionManagement.withSubProjects { subprojects }   

                        // the transitive closure of this configuration will be flattened 
                        // and added to the dependency management section
                        versionManagement.fromConfigurations { project.configurations.compile } 

                        // alternative syntax when you want to explicitly add a dependency with no transitives
                        versionManagement.withDependencies { 'manual:dep:1' }

                        // further customization of the POM is allowed if desired
                        pom.withXml { asNode().appendNode('description', 'A demonstration of maven customization') }
                    }
                }
                repositories {
                    maven {
                        // change to point to your repo, e.g. http://my.org/repo
                        url "\$buildDir/repo"
                    }
                }
            } 
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File proj1Dir = createSubProject('project1a', settingsfile, '')
        File proj2Dir = createSubProject('project2b', settingsfile, '')

        writeJavaTestClass('com.intershop.project1', proj1Dir)
        writeJavaTestClass('com.intershop.project2', proj2Dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .build()

        File pom = new File(testProjectDir, 'build/repo/com/intershop/mvn-filter/1.0.0/mvn-filter-1.0.0.pom')

        then:
        result.task(':publish').outcome == SUCCESS
        pom.text.contains('<groupId>com.intershop</groupId>')
        pom.text.contains('<artifactId>project1a</artifactId>')
        pom.text.contains('<artifactId>project2b</artifactId>')
        pom.text.contains('<groupId>manual</groupId>')
        pom.text.contains('<artifactId>dep</artifactId>')
        pom.text.contains('<version>1</version>')
    }

    def 'test publishing ivy filter with projects and filter versions'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
                id 'ivy-publish'
            }
            
            versionRecommendation {
                provider {
                    properties('list') {
                        versionMap = [
                            'com.intershop.test:testcomp1': '10.0.0',
                            'com.intershop.testglob:*': '11.0.0',
                        ]
                    }
                }
            }

            allprojects {
                apply plugin: 'java'
                apply plugin: 'ivy-publish'
                
                group = 'com.intershop'
                version = '1.0.0'

                publishing {
                    publications {
                        ivyFilter(IvyPublication) {
                            module 'ivy-filter'
                            revision project.version

                            versionManagement.withSubProjects { subprojects }     

                            // alternative syntax when you want to explicitly add a dependency with no transitives
                            versionManagement.withDependencies { [
                                        'com.intershop.test:testcomp1',
                                        'com.intershop.testglob:testglob1',
                                        'com.intershop.testglob:testglob10' ] }

                            // further customization of the POM is allowed if desired
                            descriptor.withXml { asNode().info[0].appendNode('description', 'A demonstration of maven IVY customization') }
                        }
                    }
                    repositories {
                        ivy {
                            // change to point to your repo, e.g. http://my.org/repo
                            url "\$buildDir/repo"
                            layout('pattern') {
                                ivy "${ivyPattern}"
                                artifact "${artifactPattern}"
                            }
                        }
                    }
                }                
            }
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File proj1Dir = createSubProject('project1a', settingsfile, '')
        File proj2Dir = createSubProject('project2b', settingsfile, '')

        writeJavaTestClass('com.intershop.project1', proj1Dir)
        writeJavaTestClass('com.intershop.project2', proj2Dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .build()

        File ivy = new File(testProjectDir, 'build/repo/com.intershop/ivy-filter/1.0.0/ivys/ivy-1.0.0.xml')

        then:
        result.task(':project1a:publish').outcome == SUCCESS
        result.task(':project2b:publish').outcome == SUCCESS
        result.task(':publish').outcome == SUCCESS
        ivy.text.contains('<dependency org="com.intershop" name="project1a" rev="1.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="com.intershop" name="project2b" rev="1.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="com.intershop.test" name="testcomp1" rev="10.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="com.intershop.testglob" name="testglob1" rev="11.0.0" conf="default"/>')
        ivy.text.contains('<dependency org="com.intershop.testglob" name="testglob10" rev="11.0.0" conf="default"/>')
    }

    def 'test publishing maven filter with projects and filter versions'() {
        given:
        buildFile << """
            plugins {
                id 'com.intershop.gradle.versionrecommender'
                id 'maven-publish'
            }

            versionRecommendation {
                provider {
                    properties('list') {
                        versionMap = [
                            'com.intershop.test:testcomp1': '10.0.0',
                            'com.intershop.testglob:*': '11.0.0',
                        ]
                    }
                }
            }
            
            allprojects {
                apply plugin: 'java'
                apply plugin: 'maven-publish'
                
                group = 'com.intershop'
                version = '1.0.0'

                publishing {
                    publications {
                        mvnFilter(MavenPublication) {
                            artifactId 'mvn-filter'
                            version project.version

                            versionManagement.withSubProjects { subprojects }     

                            // alternative syntax when you want to explicitly add a dependency with no transitives
                            versionManagement.withDependencies { [
                                        'com.intershop.test:testcomp1',
                                        'com.intershop.testglob:testglob1',
                                        'com.intershop.testglob:testglob10' ] }

                            // further customization of the POM is allowed if desired
                            pom.withXml { asNode().appendNode('description', 'A demonstration of maven IVY customization') }
                        }
                    }
                    repositories {
                        maven {
                            // change to point to your repo, e.g. http://my.org/repo
                            url "\$buildDir/repo"
                        }
                    }
                }                
            }
            
            repositories {
                jcenter()
            }
        """.stripIndent()

        File settingsfile = file('settings.gradle')
        settingsfile << """
            // define root proejct name
            rootProject.name = 'testProject'
        """.stripIndent()

        File proj1Dir = createSubProject('project1a', settingsfile, '')
        File proj2Dir = createSubProject('project2b', settingsfile, '')

        writeJavaTestClass('com.intershop.project1', proj1Dir)
        writeJavaTestClass('com.intershop.project2', proj2Dir)

        when:
        def result = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .build()

        File pom = new File(testProjectDir, 'build/repo/com/intershop/mvn-filter/1.0.0/mvn-filter-1.0.0.pom')

        then:
        result.task(':project1a:publish').outcome == SUCCESS
        result.task(':project2b:publish').outcome == SUCCESS
        result.task(':publish').outcome == SUCCESS
        pom.text.contains('<groupId>com.intershop</groupId>')
        pom.text.contains('<artifactId>project1a</artifactId>')
        pom.text.contains('<artifactId>project2b</artifactId>')

        pom.text.contains('<groupId>com.intershop.test</groupId>')
        pom.text.contains('<artifactId>testcomp1</artifactId>')
        pom.text.contains('<version>10.0.0</version>')
        pom.text.contains('<groupId>com.intershop.testglob</groupId>')
        pom.text.contains('<artifactId>testglob1</artifactId>')
        pom.text.contains('<artifactId>testglob10</artifactId>')
        pom.text.contains('<version>11.0.0</version>')
    }

    private String writeIvyRepo(File dir) {
        File repoDir = new File(dir, 'repo')
        File localRepoDir = new File(dir, 'localRepo')

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '1.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '1.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop', name: 'component1', rev: '1.0.0')
            module(org: 'com.intershop', name: 'component2', rev: '1.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0')

            module(org: 'com.intershop', name:'altfilter', rev: '1.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '3.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '3.1.0'
                dependency org: 'com.intershop', name: 'component3', rev: '3.2.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '10.1.0'
            }
            module(org: 'com.intershop', name: 'component1', rev: '3.0.0')
            module(org: 'com.intershop', name: 'component2', rev: '3.1.0')
            module(org: 'com.intershop', name: 'component3', rev: '3.2.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '10.1.0')

        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.other', name: 'filter', rev: '1.0.0') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '1.0.0'
                dependency org: 'com.intershop.other', name: 'depfilter', rev: '1.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0')
            module(org: 'com.intershop.other', name: 'depfilter', rev: '1.0.0') {
                dependency org: 'com.intershop.other', name: 'depcomponent1', rev: '2.0.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '10.0.0'
            }
            module(org: 'com.intershop.other', name: 'depcomponent1', rev: '1.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '10.0.0')

            module(org: 'com.intershop.other', name: 'filter', rev: '1.0.1') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '1.0.1'
                dependency org: 'com.intershop.other', name: 'component2', rev: '1.0.1'
                dependency org: 'com.intershop.other', name: 'depfilter', rev: '1.0.1'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.0.1')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.0.1')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1')
            module(org: 'com.intershop.other', name: 'depfilter', rev: '1.0.1') {
                dependency org: 'com.intershop.other', name: 'depcomponent1', rev: '1.0.1'
                dependency org: 'com.intershop.other', name: 'component2', rev: '10.0.1'
            }
            module(org: 'com.intershop.other', name: 'depcomponent1', rev: '1.0.1')
            module(org: 'com.intershop.other', name: 'component2', rev: '10.0.1')

            module(org: 'com.intershop.other', name: 'filter', rev: '1.1.0') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '1.1.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '1.1.0'
                dependency org: 'com.intershop.other', name: 'depfilter', rev: '1.1.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.1.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.1.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')
            module(org: 'com.intershop.other', name: 'depfilter', rev: '1.1.0') {
                dependency org: 'com.intershop.other', name: 'depcomponent1', rev: '1.1.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '10.1.0'
            }
            module(org: 'com.intershop.other', name: 'depcomponent1', rev: '1.1.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '10.1.0')

            module(org: 'com.intershop.other', name: 'filter', rev: '2.0.0') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '2.0.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '2.0.0'
                dependency org: 'com.intershop.other', name: 'depfilter', rev: '2.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '2.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '2.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')
            module(org: 'com.intershop.other', name: 'depfilter', rev: '2.0.0') {
                dependency org: 'com.intershop.other', name: 'depcomponent1', rev: '2.0.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '11.0.0'
            }
            module(org: 'com.intershop.other', name: 'depcomponent1', rev: '2.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '11.0.0')
        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.another', name: 'filter', rev: '1.0.0') {
                dependency org: 'com.intershop.another', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop.another', name: 'component2', rev: '1.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop.another', name: 'component1', rev: '1.0.0')
            module(org: 'com.intershop.another', name: 'component2', rev: '1.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0')

            module(org: 'com.intershop.another', name: 'filter', rev: '1.0.1') {
                dependency org: 'com.intershop.another', name: 'component1', rev: '1.0.1'
                dependency org: 'com.intershop.another', name: 'component2', rev: '1.0.1'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1'
            }
            module(org: 'com.intershop.another', name: 'component1', rev: '1.0.1')
            module(org: 'com.intershop.another', name: 'component2', rev: '1.0.1')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1')

            module(org: 'com.intershop.another', name: 'filter', rev: '1.1.0') {
                dependency org: 'com.intershop.another', name: 'component1', rev: '1.1.0'
                dependency org: 'com.intershop.another', name: 'component2', rev: '1.1.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.another', name: 'component1', rev: '1.1.0')
            module(org: 'com.intershop.another', name: 'component2', rev: '1.1.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')

            module(org: 'com.intershop.another', name: 'filter', rev: '2.0.0') {
                dependency org: 'com.intershop.another', name: 'component1', rev: '2.0.0'
                dependency org: 'com.intershop.another', name: 'component2', rev: '2.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.another', name: 'component1', rev: '2.0.0')
            module(org: 'com.intershop.another', name: 'component2', rev: '2.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')
        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop.woupdate', name: 'filter', rev: '1.0.0') {
                dependency org: 'com.intershop.woupdate', name: 'component1', rev: '1.0.0'
                dependency org: 'com.intershop.woupdate', name: 'component2', rev: '1.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop.woupdate', name: 'component1', rev: '1.0.0')
            module(org: 'com.intershop.woupdate', name: 'component2', rev: '1.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0')

            module(org: 'com.intershop.woupdate', name: 'filter', rev: '1.0.1') {
                dependency org: 'com.intershop.woupdate', name: 'component1', rev: '1.0.1'
                dependency org: 'com.intershop.woupdate', name: 'component2', rev: '1.0.1'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1'
            }
            module(org: 'com.intershop.woupdate', name: 'component1', rev: '1.0.1')
            module(org: 'com.intershop.woupdate', name: 'component2', rev: '1.0.1')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1')

            module(org: 'com.intershop.woupdate', name: 'filter', rev: '1.1.0') {
                dependency org: 'com.intershop.woupdate', name: 'component1', rev: '1.1.0'
                dependency org: 'com.intershop.woupdate', name: 'component2', rev: '1.1.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.woupdate', name: 'component1', rev: '1.1.0')
            module(org: 'com.intershop.woupdate', name: 'component2', rev: '1.1.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')

            module(org: 'com.intershop.woupdate', name: 'filter', rev: '2.0.0') {
                dependency org: 'com.intershop.woupdate', name: 'component1', rev: '2.0.0'
                dependency org: 'com.intershop.woupdate', name: 'component2', rev: '2.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.woupdate', name: 'component1', rev: '2.0.0')
            module(org: 'com.intershop.woupdate', name: 'component2', rev: '2.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')
        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '1.0.1') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.1'
                dependency org: 'com.intershop', name: 'component2', rev: '1.0.1'
            }
            module(org: 'com.intershop', name: 'component1', rev: '1.0.1')
            module(org: 'com.intershop', name: 'component2', rev: '1.0.1')

            module(org: 'com.intershop', name:'filter', rev: '2.0.0') {
                dependency org: 'com.intershop', name: 'component1', rev: '2.0.0'
                dependency org: 'com.intershop', name: 'component2', rev: '2.0.0'
            }
            module(org: 'com.intershop', name: 'component1', rev: '2.0.0')
            module(org: 'com.intershop', name: 'component2', rev: '2.0.0')

            module(org: 'com.intershop.test', name: 'testcomp1', rev: '10.0.0')
            module(org: 'com.intershop.testglob', name: 'testglob1', rev: '11.0.0')
            module(org: 'com.intershop.testglob', name: 'testglob10', rev: '11.0.0')
        }.writeTo(repoDir)

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name: 'filter', rev: '1.0.0-LOCAL') {
                dependency org: 'com.intershop', name: 'component1', rev: '1.0.0-LOCAL'
                dependency org: 'com.intershop', name: 'component2', rev: '1.0.0-LOCAL'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop', name: 'component1', rev: '1.0.0-LOCAL')
            module(org: 'com.intershop', name: 'component2', rev: '1.0.0-LOCAL')
        }.writeTo(localRepoDir)

        String repostr = """
            repositories {
                ivy {
                    name 'ivyLocal'
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${ivyPattern}"
                        artifact "${artifactPattern}"
                        artifact "${ivyPattern}"
                    }
                }
                ivy {
                    name 'ivyLocalLocal'
                    url "file://${localRepoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${ivyPattern}"
                        artifact "${artifactPattern}"
                        artifact "${ivyPattern}"
                    }
                }                
            }""".stripIndent()

        return repostr
    }

    private String writeIvyReadRepo(File dir) {
        File repoDir = new File(dir, 'readrepo')

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
            module(org: 'com.intershop', name:'filter', rev: '1.0.0') {
                dependency org: 'org.apache.logging.log4j', name: 'log4j-core', rev: '2.2'
                dependency org: 'commons-io', name: 'commons-io', rev: '2.1'
                dependency org: 'commons-codec', name: 'commons-codec', rev: '1.4'
            }

            'org.apache.logging.log4j:log4j-core:jar:2.7'
            module(org: 'com.intershop', name:'altfilter', rev: '1.0.0') {
                dependency org: 'org.apache.logging.log4j', name: 'log4j-core', rev: '2.7'
                dependency org: 'commons-io', name: 'commons-io', rev: '2.5'
                dependency org: 'commons-codec', name: 'commons-codec', rev: '1.10'
            }
        }.writeTo(repoDir)

        String repostr = """
            repositories {
                ivy {
                    name 'ivyLocal'
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${ivyPattern}"
                        artifact "${artifactPattern}"
                        artifact "${ivyPattern}"
                    }
                }
            }""".stripIndent()

        return repostr
    }
}
