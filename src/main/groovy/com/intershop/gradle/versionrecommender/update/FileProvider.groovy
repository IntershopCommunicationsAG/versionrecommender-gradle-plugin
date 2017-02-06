package com.intershop.gradle.versionrecommender.update

import groovy.util.slurpersupport.GPathResult

class FileProvider {

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
