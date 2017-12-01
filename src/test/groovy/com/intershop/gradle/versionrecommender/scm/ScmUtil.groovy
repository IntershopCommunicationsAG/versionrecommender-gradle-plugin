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
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext
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
                .setCredentialsProvider( new UsernamePasswordCredentialsProvider( System.properties['gituser'].toString(), System.properties['gitpasswd'].toString()) )
        cmd.call()
    }

    static void svnCheckOut(File target, String source) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'].toString(), System.properties['svnpasswd'].toString().toCharArray())
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
        if(status.getUntrackedFolders().size() > 0) {
            status.getUntrackedFolders().each {
                if(! status.getIgnoredNotInIndex().contains(it)) {
                    returnValue = true
                }
            }
        }
        if(status.getUntracked().size() > 0) {
            status.getUntracked().each {
                if(! status.getIgnoredNotInIndex().contains(it)) {
                    returnValue = true
                }
            }
        }

        return returnValue
    }


    static boolean svnCheckResult(File target) {
        boolean returnValue = true

        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'].toString(), System.properties['svnpasswd'].toString().toCharArray())
        try {
            svnOperationFactory.setAuthenticationManager(authenticationManager)
            SvnGetStatus statusCmd = svnOperationFactory.createGetStatus()
            statusCmd.setSingleTarget(SvnTarget.fromFile(target))
            statusCmd.setDepth(SVNDepth.INFINITY)
            statusCmd.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
                void receive(SvnTarget svnTarget, SvnStatus status) throws SVNException {
                    if(! ['build', 'gradle', '.gradle'].contains(status.path.name)) {
                        if(status.path.isDirectory() && status.path.listFiles().size() > 0) {
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_DELETED
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_ADDED
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_MISSING
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_MODIFIED
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_NONE
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_INCOMPLETE
                            returnValue &= status.getNodeStatus() != SVNStatusType.STATUS_UNVERSIONED
                        }
                    }
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
            if(it.getName() != '.git' && it.getName() != '.svn' && it.getName() != '.gitignore') {
                if (it.isDirectory()) {
                    it.deleteDir()
                }
                if (it.isFile()) {
                    it.delete()
                }
            }
        }
    }

    static void gitCommitChanges(File target, String filepattern ='.') {
        Git git = getGit(target)
        try {
            Status status = git.status().call()
            if (!status.getMissing().isEmpty() || !status.getRemoved().isEmpty()) {
                RmCommand rm = git.rm()
                status.getMissing().each {
                    rm.addFilepattern(it)
                }
                status.getRemoved().each {
                    rm.addFilepattern(it)
                }
                rm.call()
            }

            if(! status.getUntracked().isEmpty()) {
                status.getUntracked().each {
                    if(! status.getIgnoredNotInIndex().contains(it)) {
                        git.add().addFilepattern(it).call()
                    }
                }
            }
            if(! status.getUntrackedFolders().isEmpty()) {
                status.getUntrackedFolders().each {
                    if(! status.getIgnoredNotInIndex().contains(it)) {
                        git.add().addFilepattern(it).call()
                    }
                }
            }

            CommitCommand commitCmd = git.commit()
            commitCmd.setMessage('commit all changes')
            commitCmd.call()

            git.push()
                    .setCredentialsProvider(new UsernamePasswordCredentialsProvider(System.properties['gituser'].toString(), System.properties['gitpasswd'].toString()))
                    .setRemote('origin').call()

        } catch (Exception ex) {
            ex.printStackTrace()
        } finally {
            git.close()
        }
    }

    static void svnUpdate(File target) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'].toString(), System.properties['svnpasswd'].toString().toCharArray())
        svnOperationFactory.setAuthenticationManager(authenticationManager)

        SvnUpdate updateCmd = svnOperationFactory.createUpdate()
        updateCmd.setSingleTarget(SvnTarget.fromFile(target))
        updateCmd.run()
    }

    static void svnCommitChanges(File target) {
        final SvnOperationFactory svnOperationFactory = new SvnOperationFactory()
        final ISVNAuthenticationManager authenticationManager = SVNWCUtil.createDefaultAuthenticationManager(System.properties['svnuser'].toString(), System.properties['svnpasswd'].toString().toCharArray())
        svnOperationFactory.setAuthenticationManager(authenticationManager)

        try {
            checkFiles(target, svnOperationFactory)

            final SvnCommit commit = svnOperationFactory.createCommit()
            commit.setSingleTarget(SvnTarget.fromFile(target))
            commit.setCommitMessage('commit all changes')
        } catch (Exception ex) {
            ex.printStackTrace()
        }
    }

    private static void checkFiles(File target, SvnOperationFactory svnOperationFactory) throws SVNException{
        List<File> addedFiles = []
        List<File> missingFiles = []

        SvnGetStatus getStatus = svnOperationFactory.createGetStatus()
        getStatus.setSingleTarget(SvnTarget.fromFile(target))
        getStatus.setDepth(SVNDepth.INFINITY)
        getStatus.setRemote(true)
        getStatus.setReportAll(true)

        getStatus.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            void receive(SvnTarget svnTarget, SvnStatus status) throws SVNException {
                if(! status.versioned) {
                    addedFiles.add(status.path)
                }
                if(status.getNodeStatus() == SVNStatusType.STATUS_MISSING) {
                    missingFiles.add(status.path)
                }
            }
        })
        getStatus.run()

        if(addedFiles.size() > 0) {
            SvnScheduleForAddition addclient = svnOperationFactory.createScheduleForAddition()
            addclient.setDepth(SVNDepth.INFINITY)
            addclient.setAddParents(true)
            addclient.setIncludeIgnored(false)

            boolean runCmd = false

            addedFiles.each { File addFile ->
                if(! ['build', '.gradle'].contains(addFile.name)) {
                    addclient.addTarget(SvnTarget.fromFile(addFile))
                    runCmd = true
                }
            }

            if(runCmd)
                addclient.run()
        }
        if(missingFiles.size() > 0) {
            SvnScheduleForRemoval rmclient = svnOperationFactory.createScheduleForRemoval()
            rmclient.setDepth(SVNDepth.INFINITY)
            rmclient.setDeleteFiles(true)
            rmclient.setForce(true)

            missingFiles.each { File missingFile ->
                rmclient.addTarget(SvnTarget.fromFile(missingFile))
            }

            rmclient.run()
        }
    }

    private static Git getGit(File dir) {
        Repository repo = new RepositoryBuilder()
                .readEnvironment()
                .findGitDir(dir)
                .build()
        return new Git(repo)
    }
}
