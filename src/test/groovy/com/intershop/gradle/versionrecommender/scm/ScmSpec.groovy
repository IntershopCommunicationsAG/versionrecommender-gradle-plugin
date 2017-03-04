package com.intershop.gradle.versionrecommender.scm

import com.intershop.gradle.test.util.TestDir
import spock.lang.Requires
import spock.lang.Specification

class ScmSpec extends Specification {

    @TestDir
    File testDir

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'git - simple project with simple new file'() {
        setup:
            ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'master')
            File tf = new File(testDir, '.test.version')
            tf.setText('1.0.0')
            List<File> fileList = []
            fileList.add(tf)

        when:
            GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
            client.commit(fileList, 'new file added')

        then:
            ScmUtil.gitCheckResult(testDir)
            tf.exists()

        cleanup:
            ScmUtil.removeAllFiles(testDir)
            ScmUtil.gitCommitChanges(testDir)
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'git - simple project with simple changed file'() {
        setup:
        ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'master')
        File tf = new File(testDir, '.test.version')
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)
        ScmUtil.gitCommitChanges(testDir)

        when:
        GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tf.exists()
        tf.text == '1.0.0'

        when:
        File tfu = new File(testDir, '.test.version')
        tfu.setText('2.0.0')
        List<File> fileListU = []
        fileListU.add(tfu)
        client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileListU, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tfu.exists()
        tfu.text == '2.0.0'

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.gitCommitChanges(testDir)
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'git - simple project with changed configuration dir and new files'() {
        setup:
        ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'master')
        File tf = new File(testDir, 'configdir/.test.version')
        tf.getParentFile().mkdirs()
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)

        when:
        GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tf.exists()

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.gitCommitChanges(testDir)
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'git - simple project with changed configuration dir and changed files'() {
        setup:
        ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'master')
        File tf = new File(testDir, 'configdir/.test.version')
        tf.getParentFile().mkdirs()
        tf.setText('1.0.0')
        ScmUtil.gitCommitChanges(testDir)
        List<File> fileList = []
        fileList.add(tf)

        when:
        GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tf.exists()
        tf.text == '1.0.0'

        when:
        File tfu = new File(testDir, 'configdir/.test.version')
        tfu.setText('2.0.0')
        List<File> fileListU = []
        fileListU.add(tfu)
        client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileListU, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tfu.exists()
        tfu.text == '2.0.0'

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.gitCommitChanges(testDir)
    }

    @Requires({ System.properties['giturl'] &&
            System.properties['gituser'] &&
            System.properties['gitpasswd'] })
    def 'git - simple project with simple new file on a branch'() {
        setup:
        ScmUtil.gitCheckOut(testDir, System.properties['giturl'], 'newbranch')
        File tf = new File(testDir, '.test.version')
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)

        when:
        GitClient client = new GitClient(testDir, System.properties['gituser'], System.properties['gitpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.gitCheckResult(testDir)
        tf.exists()

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.gitCommitChanges(testDir)
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'svn - simple project with simple new file'() {
        setup:
        ScmUtil.svnCheckOut(testDir, System.properties['svnurl'])
        File tf = new File(testDir, '.test.version')
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)

        when:
        SvnClient client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.svnCheckResult(testDir)
        tf.exists()

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.svnCommitChanges(testDir)
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'svn - simple project with simple changed file'() {
        setup:
        ScmUtil.svnCheckOut(testDir, System.properties['svnurl'])
        File tf = new File(testDir, '.test.version')
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)
        ScmUtil.svnCommitChanges(testDir)

        when:
        SvnClient client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.svnCheckResult(testDir)
        tf.exists()
        tf.text == '1.0.0'

        when:
        File tfu = new File(testDir, '.test.version')
        tfu.setText('2.0.0')
        List<File> fileListU = []
        fileListU.add(tfu)
        client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileListU, 'file changed')

        then:
        ScmUtil.svnCheckResult(testDir)
        tfu.exists()
        tfu.text == '2.0.0'

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.svnCommitChanges(testDir)
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'svn - simple project with changed configuration dir and new files'() {
        setup:
        ScmUtil.svnCheckOut(testDir, System.properties['svnurl'])
        File tf = new File(testDir, 'configdir/.test.version')
        tf.getParentFile().mkdirs()
        tf.setText('1.0.0')
        List<File> fileList = []
        fileList.add(tf)

        when:
        SvnClient client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.svnCheckResult(testDir)
        tf.exists()

        cleanup:
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.svnCommitChanges(testDir)
    }

    @Requires({ System.properties['svnurl'] &&
            System.properties['svnuser'] &&
            System.properties['svnpasswd'] })
    def 'svn - simple project with changed configuration dir and changed files'() {
        setup:
        ScmUtil.svnCheckOut(testDir, System.properties['svnurl'])
        File tf = new File(testDir, 'configdir/.test.version')
        tf.getParentFile().mkdirs()
        tf.setText('1.0.0')
        ScmUtil.svnCommitChanges(testDir)
        List<File> fileList = []
        fileList.add(tf)

        when:
        SvnClient client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileList, 'new file added')

        then:
        ScmUtil.svnCheckResult(testDir)
        tf.exists()
        tf.text == '1.0.0'

        when:
        File tfu = new File(testDir, 'configdir/.test.version')
        tfu.setText('2.0.0')
        List<File> fileListU = []
        fileListU.add(tfu)
        client = new SvnClient(testDir, System.properties['svnuser'], System.properties['svnpasswd'])
        client.commit(fileListU, 'new file changed')

        then:
        ScmUtil.svnCheckResult(testDir)
        tfu.exists()
        tfu.text == '2.0.0'

        cleanup:
        ScmUtil.svnUpdate(testDir)
        ScmUtil.removeAllFiles(testDir)
        ScmUtil.svnCommitChanges(testDir)
    }
}
