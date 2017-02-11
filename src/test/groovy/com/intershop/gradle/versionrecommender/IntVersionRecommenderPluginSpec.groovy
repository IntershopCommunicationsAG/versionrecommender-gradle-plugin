package com.intershop.gradle.versionrecommender

import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.builder.TestIvyRepoBuilder

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
                    updateConfigItem {
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
                    updateConfigItem {
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
                    updateConfigItem {
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
                    updateConfigItem {
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
                    updateConfigItem {
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
                        versionList = [
                            'com.intershop.test:testcomp1': '10.0.0',
                            'com.intershop.testglob:*': '11.0.0',
                        ]
                    }
                }
                updateConfiguration {
                    ivyPattern = '${ivyPattern}'
                    updateConfigItem {
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

    private String writeIvyRepo(File dir) {
        File repoDir = new File(dir, 'repo')

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
