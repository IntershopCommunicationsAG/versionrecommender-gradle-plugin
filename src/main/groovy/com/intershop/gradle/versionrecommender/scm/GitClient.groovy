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
import org.eclipse.jgit.api.*
import org.eclipse.jgit.api.errors.GitAPIException
import org.eclipse.jgit.lib.Config
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.lib.RepositoryBuilder
import org.eclipse.jgit.transport.CredentialsProvider
import org.eclipse.jgit.transport.SshTransport
import org.eclipse.jgit.transport.Transport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider

@Slf4j
@CompileStatic
class GitClient {


    /*
     * Git client for all operations
     */
    private final Git gitClient

    private final File workingCopy
    private final String remoteUrl

    String userName

    String userPassword

    File keyfile

    String passphrase

    String commitMessage

    GitClient(File workingDir) {
        Repository gitRepo = new RepositoryBuilder()
                                .readEnvironment()
                                .findGitDir(workingDir)
                                .build()
        gitClient = new Git(gitRepo)


        Config config = gitRepo.getConfig()
        remoteUrl = config.getString('remote', 'origin', 'url')

    }

    String commit(List<File> fileList) {
        try {
            addMissingFiles(fileList)

            CommitCommand commitCmd = gitClient.commit()
            commitCmd.setMessage(commitMessage)
            commitCmd.call()

            PushCommand pushCmd = gitClient.push()

            initGitCommand(pushCmd)
            pushCmd.setPushAll()
            pushCmd.setPushTags()
            pushCmd.remote =  'origin'
            pushCmd.force = true
            pushCmd.call()

        } catch (GitAPIException ex) {
            throw new ScmCommitException("Commit of changes failed (${ex.getMessage()})", ex.cause)
        }
    }


    private void addMissingFiles(List<File> fileList) throws GitAPIException {
        fileList.each {
            gitClient.add().addFilepattern(it.absolutePath)
        }
    }

    private void initGitCommand(TransportCommand cmd) {
        if (remoteUrl.startsWith('http') && userName && userPassword) {
            log.debug('User name {} and password is used.', userName)
            CredentialsProvider credentials = new UsernamePasswordCredentialsProvider(userName, userPassword)
            cmd.setCredentialsProvider(credentials)
        } else if (remoteUrl.startsWith('git@') && keyfile && keyfile.exists()) {
            log.debug('ssh connector is used with key {}.', keyfile.absolutePath)
            SshConnector sshConnector = new SshConnector(keyfile, passphrase)
            cmd.setTransportConfigCallback(new TransportConfigCallback() {
                public void configure(Transport transport) {
                    SshTransport sshTransport = (SshTransport) transport
                    sshTransport.setSshSessionFactory(sshConnector)
                }
            })
        } else {
            log.error('No authentication available')
        }
    }
}
