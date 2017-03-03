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
package com.intershop.gradle.versionrecommender.scm

import org.eclipse.jgit.api.*
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import org.tmatesoft.svn.core.wc.SVNStatusType
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.*

class ScmUtil {

    // checkout
    static void gitCheckOut(File target, String source, String branch) {
        CloneCommand cmd = Git.cloneRepository()
                .setURI(source)
                .setBranch(branch)
                .setDirectory(target)
                .setCredentialsProvider( new UsernamePasswordCredentialsProvider( System.properties['gituser'], System.properties['gitpasswd']) )
        cmd.call()
    }

    static void svnCheckOut(File target, String source) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'], System.properties['svnpasswd'].toCharArray())
        try {
            svnOperationFactory.setAuthenticationManager(authenticationManager)
            final SvnCheckout checkout = svnOperationFactory.createCheckout()
            checkout.setSingleTarget(SvnTarget.fromFile(target))
            checkout.setSource(SvnTarget.fromURL(SVNURL.parseURIEncoded(source)))
            checkout.run()
        } finally {
            svnOperationFactory.dispose()
        }
    }

    static boolean gitCheckResult(File target) {
        Git git = getGit(target)
        boolean returnValue = true

        StatusCommand statusCmd = git.status()
        Status status = statusCmd.call()
        returnValue &= status.getAdded().size() == 0
        returnValue &= status.getChanged().size() == 0
        returnValue &= status.getMissing().size() == 0
        returnValue &= status.getRemoved().size() == 0
        returnValue &= status.getUntracked().size() == 0
        returnValue &= status.getUntrackedFolders().size() == 0

        return returnValue
    }


    static boolean svnCheckResult(File target) {
        boolean returnValue = true

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'], System.properties['svnpasswd'].toCharArray())
        try {
            svnOperationFactory.setAuthenticationManager(authenticationManager)
            SvnGetStatus statusCmd = svnOperationFactory.createGetStatus()
            statusCmd.setSingleTarget(SvnTarget.fromFile(target))
            statusCmd.setDepth(SVNDepth.INFINITY);
            statusCmd.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
                public void receive(SvnTarget svnTarget, SvnStatus status) throws SVNException {
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_DELETED
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_ADDED
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_MISSING
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_MODIFIED
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_NONE
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_INCOMPLETE
                    returnValue &= status.getNodeStatus() == SVNStatusType.STATUS_UNVERSIONED
                }
            })
            statusCmd.run()

        } finally {
            svnOperationFactory.dispose()
        }
        return returnValue
    }

    static void removeAllFiles(File projectDir) {
        projectDir.listFiles().each {
            if(it.isDirectory()) {
                it.deleteDir()
            }
            if(it.isFile() && it.getName() != '.gitignore') {
                it.delete()
            }
        }
    }

    static void gitCommitChanges(File projectDir, String filepattern = '.') {
        Git git = getGit(projectDir)
        try {
            git.add().addFilepattern('.').call()

            CommitCommand commitCmd = git.commit()
            commitCmd.setMessage('rollback after test')
            commitCmd.call()

            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(System.properties['gituser'], System.properties['gitpasswd']))
                    .setRemote('origin').call()

        } catch (Exception ex) {
            ex.printStackTrace()
        }
    }

    static void svnCommitChanges(File projectDir) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'], System.properties['svnpasswd'].toCharArray())
        svnOperationFactory.setAuthenticationManager(authenticationManager)

        try {
            final SvnCommit commit = svnOperationFactory.createCommit()
            commit.setSingleTarget(SvnTarget.fromFile(projectDir))
            commit.setCommitMessage('rollback after test')
            final SVNCommitInfo commitInfo = commit.run()
            println "Commit info was ${commitInfo}"
        } catch (Exception ex) {
            ex.printStackTrace()
        }
    }

    private static Git getGit(File dir) {
        Repository repo = new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(dir)
                .build()
        Git git = new Git(repo)
    }
}
