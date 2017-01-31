package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.test.util.TestDir
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TestName
import spock.lang.Specification

class RepositorySupportSpec extends Specification {

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

    def 'Ivy configuration'() {
        when:
            project.repositories {
                ivy {

                    ivyPattern('http://rnd-repo.rnd.j.intershop.de/[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml')
                    artifactPattern('http://rnd-repo.rnd.j.intershop.de/[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]')
                }
            }


        then:
            project.repositories.each {
                println it.url?.toURL().toString()

            }
    }
}
