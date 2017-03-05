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

import org.gradle.api.Project

class ScmClient implements IScmClient {

    // scm user name / token (git,svn)
    public final static String USERNAME_ENV = 'SCM_USERNAME'
    public final static String USERNAME_PRJ = 'scmUserName'

    // scm password (git,svn)
    public final static String PASSWORD_ENV = 'SCM_PASSWORD'
    public final static String PASSWORD_PRJ = 'scmUserPasswd'

    // scm key file (git)
    public final static String KEYFILE_ENV = 'SCM_KEYFILE'
    public final static String KEYFILE_PRJ = 'scmKeyFile'

    // scm passphrase (git)
    public final static String PASSPHRASE_ENV = 'SCM_KEYPASSPHRASE'
    public final static String PASSPHRASE_PRJ = 'scmKeyPassphrase'


    private IScmClient internalClient

    ScmClient(Project project) {
        internalClient = null

        // check for username and password
        String username = getVariable(project, USERNAME_ENV, USERNAME_PRJ, '')
        String password = getVariable(project, PASSWORD_ENV, PASSWORD_PRJ, '')

        // check for file and passphrase
        String keyfileStr = getVariable(project, KEYFILE_ENV, KEYFILE_PRJ, '')
        String passphrase = getVariable(project, PASSPHRASE_ENV, PASSWORD_PRJ, '')
        File keyfile = keyfileStr ? new File(keyfileStr) : null

        File gitDir = new File(project.rootDir, '.git')
        if (gitDir.exists() && gitDir.isDirectory()) {
            if(keyfile) {
                internalClient = new GitClient(project.rootDir, keyfile, passphrase)
            } else {
                internalClient = new GitClient(project.rootDir, username, password)
            }
        }

        File svnDir = new File(project.rootDir, '.svn')
        if (svnDir.exists() && svnDir.isDirectory()) {
            internalClient = new SvnClient(project.rootDir, username, password)
        }

        if(internalClient == null) {
            project.logger.quiet('No SCM client can be configured!')
        }
    }

    String commit(List<File> fileList, String commitmessage) {
        if(internalClient) {
            internalClient.commit(fileList, commitmessage)
        }
    }

    /**
     * Calculates the setting for special configuration from the system
     * or java environment or project properties.
     *
     * @param envVar        name of environment variable
     * @param projectVar    name of project variable
     * @param defaultValue  default value
     * @return              the string configuration
     */
    static String getVariable(Project project, String envVar, String projectVar, String defaultValue) {
        if(System.properties[envVar]) {
            project.logger.debug('Specified from system property {}.', envVar)
            return System.properties[envVar].toString().trim()
        } else if(System.getenv(envVar)) {
            project.logger.debug('Specified from system environment property {}.', envVar)
            return System.getenv(envVar).toString().trim()
        } else if(project.hasProperty(projectVar) && project."${projectVar}") {
            project.logger.debug('Specified from project property {}.', projectVar)
            return project."${projectVar}".toString().trim()
        }
        return defaultValue
    }
}
