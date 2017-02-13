package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.versionrecommender.util.UpdatePos
import com.intershop.release.version.Version
import com.intershop.release.version.VersionParser
import com.intershop.release.version.VersionType
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

@Slf4j
class VersionUpdater {

    private static final String versionregex = '(\\d+)(\\.\\d+)(\\.\\d+)(\\.\\d+)?'

    public String userName
    public String password

    public String ivyPattern

    private Project project

    private List<MavenArtifactRepository> mvnHttpRepList
    private List<MavenArtifactRepository> mvnFileRepList
    private List<IvyArtifactRepository> ivyHttpRepList
    private List<IvyArtifactRepository> ivyFileRepList

    VersionUpdater(Project project) {
        this.project = project
    }

    private initLists() {
        if(mvnHttpRepList == null || mvnFileRepList == null) {
            List<ArtifactRepository> mvnRepList = project.getRepositories().findAll { it instanceof MavenArtifactRepository }
            mvnHttpRepList = mvnRepList.findAll { ((MavenArtifactRepository) it).url.scheme.startsWith('http') }
            mvnFileRepList = mvnRepList.findAll { ((MavenArtifactRepository) it).url.scheme.startsWith('file') }
        }
        if(ivyHttpRepList == null || ivyFileRepList == null) {
            List<ArtifactRepository> ivyRepList = project.getRepositories().findAll { it instanceof IvyArtifactRepository }
            ivyHttpRepList = ivyRepList.findAll { ((IvyArtifactRepository) it).url?.scheme.startsWith('http') }
            ivyFileRepList = ivyRepList.findAll { ((IvyArtifactRepository) it).url?.scheme.startsWith('file') }
        }
    }

    List<String> getVersionList(String group, String module) {
        List<String> versionList = []
        initLists()

        mvnHttpRepList.any { MavenArtifactRepository repo ->
            versionList = HTTPProvider.getVersionFromMavenMetadata( ((MavenArtifactRepository)repo).getUrl().toString(), group, module, repo.credentials.username, repo.credentials.password)
            if(versionList)
                return true
        }
        if(! versionList) {
            if(ivyPattern) {
                ivyHttpRepList.any { IvyArtifactRepository repo ->
                    versionList = HTTPProvider.getVersionsFromIvyListing( ((IvyArtifactRepository)repo).getUrl().toString(), ivyPattern, group, module, repo.credentials.username, repo.credentials.password)
                    if (versionList)
                        return true
                }
                if (!versionList) {
                    ivyFileRepList.any { IvyArtifactRepository repo ->
                        versionList = FileProvider.getVersionsFromIvyListing(new File(repo.url), ivyPattern, group, module)
                        if (versionList)
                            return true
                    }
                }
            }

            if(! versionList) {
                mvnFileRepList.any { MavenArtifactRepository repo ->
                    versionList = FileProvider.getVersionFromMavenMetadata(new File(repo.url), group, module)
                    if(versionList)
                        return true
                }
            }
        }

        return versionList
    }

    String getUpdateVersion(String group, String module, String version, UpdatePos pos = UpdatePos.HOTFIX) {
        List<String> versionList = getVersionList(group, module)
        if(versionList) {
            return calculateUpdateVersion(filterVersion(versionList, version, pos), version)
        }
        return null
    }

    String getUpdateVersion(String group, String module, String version, String searchPattern, UpdatePos pos = UpdatePos.HOTFIX, String versionPattern =  searchPattern) {
        List<String> versionList = getVersionList(group, module)
        if(versionList) {
            return calculateUpdateVersion(filterVersion(versionList, version,  searchPattern, pos, versionPattern), version)
        }
        return null
    }

    String getUpdateVersion(String group, String module, String version, String patternForNextVersion, int sortStringPos) {
        List<String> versionList = getVersionList(group, module)
        if(versionList) {
            return calculateUpdateVersion(filterVersion(versionList, version, patternForNextVersion, sortStringPos), version)
        }
        return null
    }

    private static String calculateUpdateVersion(List<String> filteredVersion, String version) {
        if(filteredVersion) {
            String updateVersion = filteredVersion.last()
            if(updateVersion != version) {
                return updateVersion
            }
        }
        return null
    }

    static List<String> filterVersion(List<String> list, String version, UpdatePos pos = UpdatePos.HOTFIX) {
        int digits = 0
        String staticMetaData = ''

        String[] versionPart = version.split('-')
        def m = (versionPart[0] =~ /\d+/)
        digits = m.count

        if(versionPart.length > 1 && ! (versionPart[1] ==~ /\w+\.?\d+$/)) {
            staticMetaData = versionPart[1]
        }

        if(digits) {
            Version versionObj = VersionParser.parseVersion(version, digits != 4 ? VersionType.threeDigits : VersionType.fourDigits)
            Version nextVersion = null
            String filter = ''
            String[] versionDigit = versionPart[0].split('\\.')
            switch (pos) {
                case UpdatePos.MAJOR:
                    nextVersion = versionObj.incrementMajorVersion(staticMetaData)
                    filter = "\\d+(\\.\\d+)?(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'}"
                    break
                case UpdatePos.MINOR:
                    nextVersion = versionObj.incrementMinorVersion(staticMetaData)
                    filter = "${versionDigit[0]}(\\.\\d+)?(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'}"
                    break
                case UpdatePos.PATCH:
                    nextVersion = versionObj.incrementPatchVersion(staticMetaData)
                    filter = "${versionDigit[0]}.${versionDigit[1]}(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'}"
                    break
                case UpdatePos.HOTFIX:
                    nextVersion = digits == 4 ? versionObj.incrementHotfixVersion(staticMetaData) : versionObj.incrementPatchVersion(staticMetaData)
                    filter = "${versionDigit[0]}.${versionDigit[1]}${digits == 4 ? "\\.${versionDigit[2]}(\\.\\d+)?" : '(\\.\\d+)?'}"
                    break
            }
            if(versionPart.length > 2 || staticMetaData) {
                filter += "-${staticMetaData}"
            } else {
                filter += "(-\\w+)?"
            }

            List<Version> filteredList = list.findAll{
                it =~ /${filter}/ }.collect {
                    VersionParser.parseVersion(it, digits != 4 ? VersionType.threeDigits : VersionType.fourDigits)
                        }.findAll { it > versionObj
                            } .sort()

            List<Version> filteredList2 = filteredList.findAll { it >= nextVersion  }

            if(filteredList2.isEmpty()) {
                return filteredList.collect { getStringFromVersion(it, digits) }
            }
            return filteredList2.collect { getStringFromVersion(it, digits) }
        } else {
            throw new RuntimeException("Version '${version}' is not a semantic version.")
        }
    }

    static String getStringFromVersion(Version v, int digits) {
        if(v.getNormalVersion().versionType == VersionType.fourDigits){
            switch (digits) {
                case 3:
                    if(v.getHotfixVersion() == 0)
                        return v.toStringFor(3)
                    break
                case 2:
                    if(v.getHotfixVersion() == 0) {
                        if(v.getPatchVersion() == 0)
                            return v.toStringFor(2)
                        return v.toStringFor(3)
                    }
                    break
                case 1:
                    if(v.getHotfixVersion() == 0) {
                        if (v.getPatchVersion() == 0) {
                            if (v.getMinorVersion() == 0)
                                return v.toStringFor(1)
                            return v.toStringFor(2)
                        }
                        return v.toStringFor(3)
                    }
                    break
            }
        } else {
            switch (digits) {
                case 2:
                    if(v.getPatchVersion() == 0)
                        return v.toStringFor(2)
                    break
                case 1:
                    if(v.getPatchVersion() == 0) {
                        if(v.getMinorVersion() == 0)
                            return v.toStringFor(1)
                        return v.toStringFor(2)
                    }
                    break
            }
        }
        return v.toString()
    }

    static List<String> filterVersion(List<String> list, String version, String searchExtPattern, UpdatePos pos = UpdatePos.HOTFIX, String versionExtPattern = searchExtPattern) {
        int digits = 0

        def versionExtension = (version =~ /${versionExtPattern}/)

        if(! versionExtension.count) {
            throw new RuntimeException("Pattern '${versionExtension} for version extension does not match to specified version '${version}'. Please specify a separate pattern for specified version.")
        }
        String semVersion = (version - versionExtension[0])

        def m = (semVersion =~ /\d+/)
        digits = m.count

        if(digits) {
            VersionType type = digits != 4 ? VersionType.threeDigits : VersionType.fourDigits

            Version versionObj = VersionParser.parseVersion(semVersion, type)
            Version nextVersion = null
            String filter = ''
            String[] versionDigit = semVersion.split('\\.')
            switch (pos) {
                case UpdatePos.MAJOR:
                    nextVersion = versionObj.incrementMajorVersion()
                    filter = "(\\d+(\\.\\d+)?(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'})"
                    break
                case UpdatePos.MINOR:
                    nextVersion = versionObj.incrementMinorVersion()
                    filter = "(${versionDigit[0]}(\\.\\d+)?(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'})"
                    break
                case UpdatePos.PATCH:
                    nextVersion = versionObj.incrementPatchVersion()
                    filter = "(${versionDigit[0]}.${versionDigit[1]}(\\.\\d+)?${digits != 4 ? '' : '(\\.\\d+)?'})"
                    break
                case UpdatePos.HOTFIX:
                    nextVersion = digits == 4 ? versionObj.incrementHotfixVersion() : versionObj.incrementPatchVersion()
                    filter = "(${versionDigit[0]}.${versionDigit[1]}${digits == 4 ? "\\.${versionDigit[2]}(\\.\\d+)?" : '(\\.\\d+)?'})"
                    break
            }
            filter += "${searchExtPattern}"

            Map<Version, String> versionMap = [:]

            List<Version> filteredList = list.findAll{ it =~ /${filter}/ }.each {String vs ->
                def vsm = (vs =~ /${filter}/)
                try {
                    versionMap.put(VersionParser.parseVersion(vsm[0][1], type), vs)
                }catch (Exception ex) {
                    log.debug('It was not possible to parse version {}', vs)
                }
            }

            List<Version> filteredKeys = versionMap.keySet().findAll {
                it >= versionObj
            } .sort()

            List<Version> filteredKeys2 = versionMap.keySet().findAll {
                it >= nextVersion
            } .sort()

            if(filteredKeys2.isEmpty()) {
                return versionMap.subMap(filteredKeys).values() as List<String>
            } else {
                return versionMap.subMap(filteredKeys2).values() as List<String>
            }
        } else {
            throw new RuntimeException("Version '${version}' ('${semVersion}') is not a semantic version.")
        }
    }

    static List<String> filterVersion(List<String> list, String version, String patternForNextVersion, int sortStringPos) {
        def mv = (version =~ /${patternForNextVersion}/)
        if(mv.count < 1) {
            throw new RuntimeException("Pattern for next version '${patternForNextVersion}' does not match to version '${version}'.")
        }
        String sortStr = ''
        try {
            sortStr = mv[0][sortStringPos]
        } catch (Exception ex) {
            throw new RuntimeException("Group '${sortStringPos}' with pattern '${patternForNextVersion}' does not exists for version '${version}'.")
        }
        if(! sortStr) {
            throw new RuntimeException("Group '${sortStringPos}' with pattern '${patternForNextVersion}' is empty for version '${version}'.")
        }

        boolean isLong = false
        try {
            isLong = (sortStr.toLong().toString() == sortStr)
        } catch(Exception ex) {
            log.debug('{} of {} ist not a number.', sortStr, version )
        }

        List<String> filteredList = list.findAll { it =~ /${patternForNextVersion}/ }.sort {a, b ->
            def m1 = (a =~ /${patternForNextVersion}/)
            def m2 = (a =~ /${patternForNextVersion}/)
            m1[0][sortStringPos] <=> m2[0][sortStringPos]
        }.findAll {
            def m = (it =~ /${patternForNextVersion}/)
            if(isLong) {
                try {
                    return sortStr.toLong() < "${m[0][sortStringPos]}".toLong()
                } catch(Exception ex) {
                    return false
                }
            } else {
                return sortStr < m[0][sortStringPos]
            }
        }

        return filteredList
    }
}
