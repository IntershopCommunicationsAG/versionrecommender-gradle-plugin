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

/**
 * This class provides methods to collect all available version in file based repositories.
 */
@CompileStatic
class FileVersionProvider {

    /**
     * Collect a list of all available versions from a file based Maven repository.
     *
     * @param repo          Repository directory
     * @param group         Group of the module
     * @param artifactid    Artifact ID of the module
     * @return              a list of available versions
     */
    static List<String> getVersionFromMavenMetadata(File repo, String group, String artifactid) {
        File metadataFile = new File(repo, "/${group.replace('.', '/')}/${artifactid}/maven-metadata.xml")
        if(metadataFile.exists()) {
            return MavenMetadataHelper.getVersionList(metadataFile)
        } else {
            return null
        }
    }

    /**
     * Collect a list of all available versions from a file based Ivy repository.
     *
     * @param repo      Repository directory
     * @param pattern   Ivy layout pattern
     * @param org       Organisation of the module
     * @param name      Name of the module
     * @return          a list of available versions
     */
    @CompileStatic
    static List<String> getVersionsFromIvyListing(File repo, String pattern, String org, String name) {
        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', org.replaceAll('/','.')).replaceAll('\\[module]', name)
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
