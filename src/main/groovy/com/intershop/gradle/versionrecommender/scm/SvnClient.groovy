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

import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.tmatesoft.svn.core.SVNCommitInfo
import org.tmatesoft.svn.core.SVNDepth
import org.tmatesoft.svn.core.SVNException
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions
import org.tmatesoft.svn.core.internal.wc17.SVNWCContext
import org.tmatesoft.svn.core.internal.wc2.compat.SvnCodec
import org.tmatesoft.svn.core.wc.SVNStatus
import org.tmatesoft.svn.core.wc.SVNWCUtil
import org.tmatesoft.svn.core.wc2.*

@Slf4j
@CompileStatic
class SvnClient implements ScmClient {

    private final SvnOperationFactory svnOpFactory
    private final File workingCopy

    String userName

    String userPassword

    String commitMessage

    SvnClient(File workingDir) {
        this.workingCopy = workingDir
        svnOpFactory = new SvnOperationFactory()
    }

    String commit(List<File> fileList) {
        try {
            initSvnOpFactory()

            addMissingFiles(fileList)

            final SvnCommit commit = svnOpFactory.createCommit()
            commit.setSingleTarget(SvnTarget.fromFile(workingCopy))
            commit.setCommitMessage(commitMessage)
            final SVNCommitInfo commitInfo = commit.run()
            if(commitInfo.newRevision > 0) {
                return Long.toString(commitInfo.newRevision)
            } else {
                return ''
            }
        } catch (SVNException ex) {
            throw new ScmCommitException("Commit of changes failed (${ex.getMessage()})", ex.cause)
        }
    }

    private void initSvnOpFactory() {
        if(userName && userPassword) {
            log.debug('Add username / password authentication manager')
            svnOpFactory.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager(userName, userPassword.toCharArray()))
        } else {
            svnOpFactory.setAuthenticationManager(SVNWCUtil.createDefaultAuthenticationManager())
        }

        svnOpFactory.setOptions(new DefaultSVNOptions())
    }

    private void addMissingFiles(List<File> fileList) throws SVNException{
        List<File> missingFiles = []

        final SVNWCContext context = new SVNWCContext(svnOpFactory.getOptions(), svnOpFactory.getEventHandler());

        SvnGetStatus getStatus = svnOpFactory.createGetStatus()
        getStatus.setSingleTarget(SvnTarget.fromFile(workingCopy))
        getStatus.setDepth(SVNDepth.INFINITY)
        getStatus.setRemote(true)
        getStatus.setReportAll(true)

        getStatus.setReceiver(new ISvnObjectReceiver<SvnStatus>() {
            public void receive(SvnTarget target, SvnStatus status) throws SVNException {
                final SVNStatus oldStatus = SvnCodec.status(context, status)
                if(! status.versioned && fileList.findAll { it.absolutePath.startsWith(status.path.absolutePath) }.size() > 0) {
                    missingFiles.add(status.path)
                }
            }
        })
        getStatus.run()

        if(missingFiles.size() > 0) {
            SvnScheduleForAddition addclient = svnOpFactory.createScheduleForAddition()
            addclient.setAddParents(true)
            addclient.setDepth(SVNDepth.INFINITY)
            addclient.setAddParents(true)

            missingFiles.each { File missingFile ->
                addclient.addTarget(SvnTarget.fromFile(missingFile))
            }

            addclient.run()
        }
    }
}
