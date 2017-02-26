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

class ScmSupport {

    ScmSupport(Project project) {

    }

    private String initScmType(Project project) {
        File gitDir = new File(project.rootDir, '.git')
        if (gitDir.exists() && gitDir.isDirectory()) {
            return 'git'
        }

        File svnDir = new File(project.rootDir, '.svn')
        if (svnDir.exists() && svnDir.isDirectory()) {
            return 'svn'
        }
        return null
    }

}
