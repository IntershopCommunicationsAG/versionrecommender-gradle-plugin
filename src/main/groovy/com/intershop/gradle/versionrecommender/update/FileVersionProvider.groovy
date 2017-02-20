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
package com.intershop.gradle.versionrecommender.update

import groovy.transform.CompileStatic
import groovy.util.slurpersupport.GPathResult

class FileVersionProvider {

    public static List<String> getVersionFromMavenMetadata(File repo, String group, String module) {
        File metadataFile = new File(repo, "/${group.replace('.', '/')}/${module}/maven-metadata.xml")
        if(metadataFile.exists()) {
            GPathResult modelMetaData = new XmlSlurper().parse(metadataFile)
            List<String> list = []
            modelMetaData.versioning.versions.version.each{
                list.add(it.toString())
            }
            return list
        } else {
            return null
        }
    }

    @CompileStatic
    public static List<String> getVersionsFromIvyListing(File repo, String pattern, String group, String module) {
        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', group.replaceAll('/','.')).replaceAll('\\[module]', module)
        File versionDir = new File(repo, path)

        if(versionDir.exists()) {
            List<String> list = []
            versionDir.eachDir {
                list.add(it.name)
            }
            return list
        } else {
            return null
        }
    }
}
