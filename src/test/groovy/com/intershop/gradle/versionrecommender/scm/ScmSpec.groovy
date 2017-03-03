package com.intershop.gradle.versionrecommender.scm

import com.intershop.gradle.test.util.TestDir
import spock.lang.Specification

class ScmSpec extends Specification {

    @TestDir
    File testDir

    def 'git - simple project with simple new file'() {
        setup:
            ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'master')
            File tf = new File(testDir, '.test.version')
            List<File> fileList = []
            fileList.add(tf)

        when:
            GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
            client.commit(fileList, 'new file added')

        then:
            ScmUtil.gitCheckResult(testDir)

        cleanup:
            ScmUtil.removeAllFiles(testDir)
            ScmUtil.gitCommitChanges(testDir)
    }

    def 'git - simple project with simple changed file'() {

    }

    def 'git - simple project with changed properties file'() {

    }

    def 'git - simple project with changed configuration dir and new files'() {

    }

    def 'git - simple project with changed configuration dir and changed files'() {

    }

    def 'git - simple project with simple new file on a branch'() {

    }
}
