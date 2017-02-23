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

import com.intershop.gradle.versionrecommender.util.ConfigurationException
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException

import java.util.regex.Matcher

/**
 * This class provides methods to collect all available version in remote based repositories.
 * Only http and https is supported.
 */
@Slf4j
class HTTPVersionProvider {

    private static final int DEFAULT_PROXY_PORT = -1

    /**
     * Collect a list of all available versions from a remote based Maven repository.
     *
     * @param repo          Repository URL (http/https based)
     * @param group         Group of the module
     * @param artifactid    Artifact ID of the module
     * @param username      Username of repository credentials (Default is an empty string.)
     * @param password      Password of repository credentials (Default is an empty string.)
     * @return              a list of available versions
     */
    public static List<String> getVersionFromMavenMetadata(String repo, String group, String artifactid, String username = '', String password = '') {
        List<String> versions = []
        Map<String, String> hostPath = getHostPath(repo)

        HTTPBuilder http = getHttpBuilder(hostPath.host , username, password)
        http.parser.'application/unknown' = http.parser.'application/xml'

        try {
            versions = http.get(
                    path: "${hostPath.path ?: ''}/${group.replace('.', '/')}/${artifactid}/maven-metadata.xml",
                    contentType: ContentType.XML) { resp, xml ->
                if (!xml) {
                    return []
                }
                try {
                    return xml.versioning.versions.version
                } catch (Exception e) {
                    log.error("Exception occurred while trying to fetch versions. The fetched XML is [$xml]".toString(), e)
                    return []
                }
            }.collect { it.toString() }
        } catch (HttpResponseException respEx) {
            log.info('{}:{} not found in {}', group, artifactid, repo )
        }

        return versions
    }

    /**
     * Collect a list of all available versions from a remote based Ivy repository.
     *
     * @param repo          Repository URL (http/https based)
     * @param pattern       Ivy layout pattern
     * @param org           Organisation of the module
     * @param name          Name of the module
     * @param username      Username of repository credentials (Default is an empty string.)
     * @param password      Password of repository credentials (Default is an empty string.)
     * @return              a list of available versions
     */
    public static List<String> getVersionsFromIvyListing(String repo, String pattern, String org, String name, String username = '', String password = '') {
        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', org.replaceAll('/','.')).replaceAll('\\[module]', name)
        List<String> versions = []

        HTTPBuilder http = getHttpBuilder("${repo}${(repo.endsWith("/") ? '' : '/')}${path}", username, password)

        try {
            versions = http.get(contentType: ContentType.HTML) { resp, html ->
                if(!html) {
                    return []
                }
                try {
                    return html."**".findAll { it.@href.toString().endsWith('/') && ! it.@href.toString().startsWith('..')}.collect {
                        ((NodeChild)it).text().replace('/','')
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while trying to fetch versions. The fetched HTML is [$html]".toString(), e)
                    return []
                }
            }.collect { it.toString() }
        } catch (HttpResponseException respEx) {
            log.info('{}:{} not found in {}', org, name, repo )
        }

        return versions
    }

    /**
     * Provides an configured HTTP(S) client
     *
     * @param repo          Repository URL (http/https based)
     * @param username      Username of repository credentials (Default is an empty string.)
     * @param password      Password of repository credentials (Default is an empty string.)
     * @return              configured http(s) client
     */
    private static HTTPBuilder getHttpBuilder(String repo, String username = '', String password = '') {
        HTTPBuilder http = new HTTPBuilder(repo)
        http.ignoreSSLIssues()

        setProxySettings(http)

        if(username && password) {
            log.info('User {} is used for access.', username)
            http.setHeaders([Authorization: "Basic ${"${username}:${password}".bytes.encodeBase64().toString()}"])
        }
        return http
    }

    /**
     * Adds a proxy configuration if system variables are available.
     *
     * @param http pre cconfigured http(s) client
     */
    private static void setProxySettings(HTTPBuilder http) {

        String scheme = new URL(http.uri.toString()).getProtocol().toString()
        String hostname = System.getProperty("${scheme}.proxyHost")
        String port =  System.getProperty("${scheme}.proxyPort")

        if(port && hostname) {
            log.info('Proxy host is used: {}://{}:{}', scheme, hostname, port)
            http.setProxy(hostname, port, scheme)
        }
    }

    /**
     * Calculates the host url and the path from a given String.
     *
     * @param repodef
     * @return Map with two keys - host and path
     */
    private static Map<String, String> getHostPath(repodef) {
        Matcher hostPathMatcher = repodef =~ /^(.*:)\/\/([A-Za-z0-9\-\.]+)(:[0-9]+)?(.*)$/
        if(hostPathMatcher.matches()){
            String hostPath = hostPathMatcher.group(4) != '/' ? hostPathMatcher.group(4) : ''
            return [host: "${hostPathMatcher.group(1)}//${hostPathMatcher.group(2)}${hostPathMatcher.group(3) ?: ''}",
                    path: hostPath]
        } else {
            throw new ConfigurationException("${repodef} is not a host url!")
        }
    }
}
