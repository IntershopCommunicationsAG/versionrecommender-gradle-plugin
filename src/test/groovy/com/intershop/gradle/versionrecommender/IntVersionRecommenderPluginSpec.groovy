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
                testConfig 'com.intershop:component1'
                testConfig 'com.intershop.other:component1'
                testConfig 'com.intershop.another:component1'
                testConfig 'com.intershop.woupdate:component1'
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
        resultUpdate.task(':update').outcome == SUCCESS
        ! storeFileFilter5.exists()
        storeFileFilter4.exists()
        storeFileFilter3.exists()
        storeFileFilter2.exists()
        ! storeFileFilter1.exists()
        storeFileFilter4.text == '1.0.1'
        storeFileFilter3.text == '1.0.1'
        storeFileFilter2.text == '1.0.1'
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
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.0')

            module(org: 'com.intershop.other', name: 'filter', rev: '1.0.1') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '1.0.1'
                dependency org: 'com.intershop.other', name: 'component2', rev: '1.0.1'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.0.1')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.0.1')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.1.1')

            module(org: 'com.intershop.other', name: 'filter', rev: '1.1.0') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '1.1.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '1.1.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '1.1.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '1.1.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')

            module(org: 'com.intershop.other', name: 'filter', rev: '2.0.0') {
                dependency org: 'com.intershop.other', name: 'component1', rev: '2.0.0'
                dependency org: 'com.intershop.other', name: 'component2', rev: '2.0.0'
                dependency org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0'
            }
            module(org: 'com.intershop.other', name: 'component1', rev: '2.0.0')
            module(org: 'com.intershop.other', name: 'component2', rev: '2.0.0')
            module(org: 'org.apache.tomcat', name: 'tomcat-catalina', rev: '9.2.0')
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
}
