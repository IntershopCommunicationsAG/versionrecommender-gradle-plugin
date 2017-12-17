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
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild

import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import java.security.SecureRandom

/**
 * This class provides methods to collect all available version in remote based repositories.
 * Only http and https is supported.
 */
@CompileStatic
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
    static List<String> getVersionFromMavenMetadata(String repo, String group, String artifactid, String username = '', String password = '') {
        List<String> versions = []
        String url = "${repo}${(repo.endsWith("/") ? '' : '/')}${group.replace('.', '/')}/${artifactid}/maven-metadata.xml"
        try {
            HttpURLConnection conn = getUrlConnection(url)
            setAuthorization(conn, username, password)
            conn.setRequestProperty("Content-Type", "application/xml")

            if(conn.getResponseCode() == 200) {
                try {
                    String metaString = conn.getContent().toString()
                    if (metaString) {
                        versions = MavenMetadataHelper.getVersionList(conn.getContent())
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while trying to fetch versions. {}", e)
                    return []
                }
            } else {
                log.info('{}:{} not found in {} - http return code was {}', group, artifactid, repo, conn.responseCode)
            }
        } catch (IOException ex) {
            log.info('{}:{} not found in {} - IOException: {}', group, artifactid, repo, ex.getMessage() )
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
     @TypeChecked(TypeCheckingMode.SKIP)
     static List<String> getVersionsFromIvyListing(String repo, String pattern, String org, String name, String username = '', String password = '') {
        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', org.replaceAll('/','.')).replaceAll('\\[module]', name)
        List<String> versions = []

        String url = "${repo}${(repo.endsWith("/") ? '' : '/')}${path}"
        try {
            def conn = getUrlConnection(url)
            setAuthorization(conn, username, password)
            conn.setRequestProperty("Content-Type", "application/html")

            if (conn.responseCode == 200) {
                def html = new XmlSlurper(new org.cyberneko.html.parsers.SAXParser()).parseText(conn.content.text)
                try {
                    return html."**".findAll { it.@href.toString().endsWith('/') && ! it.@href.toString().startsWith('..')}.collect {
                        ((NodeChild)it).text().replace('/','')
                    }
                } catch (Exception e) {
                    log.error("Exception occurred while trying to fetch versions. The fetched HTML is [$html]".toString(), e)
                    return []
                }
            }
        }catch (IOException ex) {
            log.info('{}:{} not found in {} - IOException: {}', org, name, repo, ex.getMessage() )
        }

        return versions
    }

    /**
     * Provides an configured HTTP(S) client
     *
     * @param conn          Url connection for further configuration
     * @param username      Username of repository credentials (Default is an empty string.)
     * @param password      Password of repository credentials (Default is an empty string.)
     * @return              configured http(s) client
     */
    private static void setAuthorization(URLConnection conn, String username = '', String password = '') {
        if(username && password) {
            log.info('User {} is used for access.', username)

            String authString = "${username}:${password}".getBytes().encodeBase64().toString()
            conn.setRequestProperty( "Authorization", "Basic ${authString}" )
        }
    }

    /**
     * Creates the connection, adds a proxy configuration if system variables are available
     * and configureds https if necessary
     *
     * @param url  URL of the download artifact
     */
    private static HttpURLConnection getUrlConnection(String url) throws IOException {

        URL urlInternal = url.toURL()

        String scheme = urlInternal.getProtocol()
        String hostname = System.getProperty("${scheme}.proxyHost")
        String port =  System.getProperty("${scheme}.proxyPort")

        if(scheme == 'https') {
            SSLContext sc = SSLContext.getInstance("SSL")
            Map trustAll = [getAcceptedIssuers: {}, checkClientTrusted: { a, b -> }, checkServerTrusted: { a, b -> }]
            sc.init(null, [trustAll as X509TrustManager] as TrustManager[], new SecureRandom())
            HttpsURLConnection.defaultSSLSocketFactory = sc.socketFactory
        }

        if(port && hostname) {
            log.info('Proxy host is used: {}://{}:{}', scheme, hostname, port)
            Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(hostname, Integer.parseInt(port)))
            return (HttpURLConnection) urlInternal.openConnection(proxy)
        } else {
            return (HttpURLConnection) urlInternal.openConnection()
        }
    }
}
