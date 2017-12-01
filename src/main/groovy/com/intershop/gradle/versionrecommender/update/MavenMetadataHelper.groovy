package com.intershop.gradle.versionrecommender.update

import groovy.transform.CompileStatic
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

@CompileStatic
class MavenMetadataHelper {

    protected static List<String> getVersionList(Object metadata) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance()
        DocumentBuilder builder = dbFactory.newDocumentBuilder()

        Document meta = metadata instanceof File ? builder.parse((File)metadata) : builder.parse((InputStream) metadata)


        meta.getDocumentElement().normalize()

        meta.getDocumentElement().normalize()

        NodeList versioning = meta.getElementsByTagName('versioning')
        NodeList versions = versioning.length > 0 ? ((Element)versioning.item(0)).getElementsByTagName('versions'): null
        NodeList version = versions.length > 0 ? ((Element)versions.item(0)).getElementsByTagName('version'): null

        List<String> list = []

        if(version && version.length > 0) {
            for (int temp = 0; temp < version.getLength(); temp++) {
                list.add(version.item(temp).getTextContent())
            }
        }
        return list
    }


    private static List<String> getVersionListFrom(Document meta) {

    }
}
