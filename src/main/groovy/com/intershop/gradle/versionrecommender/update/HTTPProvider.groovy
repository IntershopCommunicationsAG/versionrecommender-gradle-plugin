package com.intershop.gradle.versionrecommender.update

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import org.apache.commons.codec.binary.Base64

@Slf4j
class HTTPProvider {

    private static final int DEFAULT_PROXY_PORT = -1

    public static List<String> getVersionFromMavenMetadata(String repo, String group, String module, String username = '', String password = '') {
        HTTPBuilder http = getHttpBuilder(repo, username, password)

        http.parser.'application/unknown' = http.parser.'application/xml'

        List<String> versions = http.get(
                path: "/${group.replace('.', '/')}/${module}/maven-metadata.xml",
                contentType: ContentType.XML) { resp, xml ->
            if(!xml) {
                return []
            }
            try {
                return xml.versioning.versions.version
            } catch (Exception e) {
                log.error("Exception occurred while trying to fetch versions. The fetched XML is [$xml]".toString(), e)
                return []
            }
        } as List<String>

        return versions
    }

    public static List<String> getVersionsFromIvyListing(String repo, String pattern, String group, String module, String username = '', String password = '') {

        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', group.replaceAll('/','.')).replaceAll('\\[module]', module)

        HTTPBuilder http = getHttpBuilder("${repo}${(repo.endsWith("/") ? '' : '/')}${path}/", username, password)

        List<String> versions = http.get(contentType: ContentType.HTML) { resp, html ->
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
        } as List<String>

        return versions
    }

    private static HTTPBuilder getHttpBuilder(String repo, String username = '', String password = '') {
        HTTPBuilder http = new HTTPBuilder(repo)
        http.ignoreSSLIssues()

        int i = repo.indexOf(':')
        setProxySettings(http)

        if(username && password) {
            byte[] authEncBytes = Base64.encodeBase64("${username}:${password}".getBytes())
            authEncBytes.toString()
            http.setHeaders([Authorization: "Basic ${authEncBytes.toString()}"])
        }
        return http
    }

    private static void setProxySettings(HTTPBuilder http) {

        String scheme = ((URI) http.getUri()).toURL().getProtocol().toString()

        String hostname = System.getProperty("${scheme}.proxyHost")
        String port =  System.getProperty("${scheme}.proxyPort")

        if(port && hostname) {
            http.setProxy(hostname, port, scheme)
        }
    }

}
