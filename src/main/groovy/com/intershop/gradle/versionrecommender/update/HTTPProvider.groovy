package com.intershop.gradle.versionrecommender.update

import groovy.util.logging.Slf4j
import groovy.util.slurpersupport.NodeChild
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.HttpResponseException

@Slf4j
class HTTPProvider {

    private static final int DEFAULT_PROXY_PORT = -1

    public static List<String> getVersionFromMavenMetadata(String repo, String group, String module, String username = '', String password = '') {
        List<String> versions = []

        HTTPBuilder http = getHttpBuilder(repo, username, password)
        http.parser.'application/unknown' = http.parser.'application/xml'

        try {
            versions = http.get(
                    path: "/${group.replace('.', '/')}/${module}/maven-metadata.xml",
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
            log.info('{}:{} not found in {}', group, module, repo )
        }

        return versions
    }

    public static List<String> getVersionsFromIvyListing(String repo, String pattern, String group, String module, String username = '', String password = '') {
        int i = pattern.indexOf('[revision]')
        String path = pattern.substring(0, i - 1).replaceAll('\\[organisation]', group.replaceAll('/','.')).replaceAll('\\[module]', module)
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
            log.info('{}:{} not found in {}', group, module, repo )
        }

        return versions
    }

    private static HTTPBuilder getHttpBuilder(String repo, String username = '', String password = '') {
        HTTPBuilder http = new HTTPBuilder(repo)
        http.ignoreSSLIssues()

        setProxySettings(http)

        if(username && password) {
            http.setHeaders([Authorization: "Basic ${"${username}:${password}".bytes.encodeBase64().toString()}"])
        }
        return http
    }

    private static void setProxySettings(HTTPBuilder http) {

        String scheme = new URL(http.uri.toString()).getProtocol().toString()
        String hostname = System.getProperty("${scheme}.proxyHost")
        String port =  System.getProperty("${scheme}.proxyPort")

        if(port && hostname) {
            http.setProxy(hostname, port, scheme)
        }
    }

}
