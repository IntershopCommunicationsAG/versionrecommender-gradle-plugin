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

        Document meta = metadata instanceof File ? builder.parse((File) metadata) : builder.parse((InputStream) metadata)


        meta.getDocumentElement().normalize()

        meta.getDocumentElement().normalize()

        NodeList versioning = meta.getElementsByTagName('versioning')
        NodeList versions = versioning.length > 0 ? ((Element) versioning.item(0)).getElementsByTagName('versions') : null
        NodeList version = versions.length > 0 ? ((Element) versions.item(0)).getElementsByTagName('version') : null

        List<String> list = []

        if (version && version.length > 0) {
            for (int temp = 0; temp < version.getLength(); temp++) {
                list.add(version.item(temp).getTextContent())
            }
        }
        return list
    }
}
