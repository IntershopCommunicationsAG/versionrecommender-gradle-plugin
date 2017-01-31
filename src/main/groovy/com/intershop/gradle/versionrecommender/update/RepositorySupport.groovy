package com.intershop.gradle.versionrecommender.update

import groovy.util.logging.Slf4j
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

@Slf4j
class RepositorySupport {

    List<String> getVersionsFormIvyRepo(IvyArtifactRepository ivyRepo, String pattern ,String group, String name, String username = '', String password = '') {
        if(ivyRepo.url) {
            String urlStr = ivyRepo.url.toURL().toString()
            if (urlStr.startsWith('file:')) {
                return FileProvider.getVersionsFromIvyListing(new File(ivyRepo.url), pattern, group, name)
            } else if (urlStr.startsWith('http:') || urlStr.startsWith('https:')) {
                return HTTPProvider.getVersionsFromIvyListing(urlStr, pattern, group, name, username, password)
            } else {
                log.error('This kind of Ivy repository is not supported.')
                return null
            }
        } else {
            log.error('It is not possible to identify the URL for {}', ivyRepo.name)
        }
    }

    List<String> getVersionsFormMvnRepo(MavenArtifactRepository mvnRepo, String group, String name, String username = '', String password = '') {
        if(mvnRepo.url) {
            String urlStr = mvnRepo.url.toURL().toString()
            if (urlStr.startsWith('file:')) {
                return FileProvider.getVersionFromMavenMetadata(new File(mvnRepo.url), group, name)
            } else if (urlStr.startsWith('http:') || urlStr.startsWith('https:')) {
                return HTTPProvider.getVersionFromMavenMetadata(urlStr, group, name, username, password)
            } else {
                log.error('This kind of Maven repository is not supported.')
                return null
            }
        } else {
            log.error('It is not possible to identify the URL for {}', mvnRepo.name)
        }
    }
}
