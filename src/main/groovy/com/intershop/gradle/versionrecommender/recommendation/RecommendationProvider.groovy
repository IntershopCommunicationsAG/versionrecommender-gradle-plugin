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


package com.intershop.gradle.versionrecommender.recommendation

import com.intershop.gradle.versionrecommender.extension.VersionRecommenderExtension
import com.intershop.gradle.versionrecommender.recommendation.VersionProvider
import com.intershop.gradle.versionrecommender.util.UpdatePos
import com.intershop.gradle.versionrecommender.util.VersionExtension
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import java.util.regex.Pattern

@CompileStatic
@Slf4j
abstract class RecommendationProvider implements VersionProvider {

    private String name
    private File workingDir
    private File configDir

    //cache for patterns
    protected Map<Pattern, String> globs

    protected boolean transitive = false
    protected boolean override = false
    protected Project project
    protected VersionExtension versionExtension
    protected UpdatePos updatePos
    protected Map<String, String> versions = null

    RecommendationProvider(String name, Project project) {
        this.name = name
        this.project = project

        this.updatePos = UpdatePos.NONE

        setWorkingDir(new File(project.getRootProject().getBuildDir(), VersionRecommenderExtension.EXTENSIONNAME))
        setConfigDir(project.getRootProject().getProjectDir())

        versionMap = [:]
        globs = new HashMap<Pattern, String>()
    }

    String getName() {
        return this.name
    }

    Map<String, String> versionMap

    @Override
    boolean isAdaptable() {
        return false
    }

    @Override
    void setTransitives(boolean transitive) {
        this.transitive = transitive
    }

    @Override
    void setOverrideTransitives(boolean override) {
        this.override = override
    }

    @Override
    void setVersionExtension(VersionExtension versionExtension) {
        this.versionExtension = versionExtension
    }

    // Working Dir
    @Override
    void setWorkingDir(File workingDir) {
        this.workingDir = workingDir
        if(! workingDir.exists())
            workingDir.mkdirs()
    }

    // Config Dir
    @Override
    void setConfigDir(File configDir) {
        this.configDir = configDir
        if(! configDir.exists())
            configDir.mkdirs()
    }

    File getWorkingDir() {
        return this.workingDir
    }

    File getConfigDir() {
        return this.configDir
    }

    abstract void fillVersionMap()

    abstract boolean isVersionRequired()

    String getVersionPropertyName() {
        return "${this.getName()}Version"
    }

    String getVersionFromProperty() {
        String versionProperty = project.findProperty(getVersionPropertyName()) ?: ''
        return versionProperty
    }

    @Override
    String getVersion(String org, String name) {
        String version = ''
        if(versions == null) {
            versions = [:]

            fillVersionMap()

            if (versionMap) {
                getVersionMap().each { String k, String v ->
                    if(k.contains('*')) {
                        globs.put(Pattern.compile(k.replaceAll("\\*", ".*?")), v)
                    } else {
                        versions.put(k, v)
                    }
                    if (transitive && !k.contains('*')) {
                        calculateDependencies(k, v)
                    }
                }
            }
        }

        if(versions != null)
            version = versions.get("${org}:${name}".toString())

        if(version)
            return version

        if(!globs.isEmpty()) {
            String key = "${org}:${name}"
            globs.any { Pattern p, String gv ->
                if (p.matcher(key).matches()) {
                    version = gv
                    return true
                }
            }
        }

        return version
    }

    String getTaskName(String prefix) {
        return "${prefix}${getName().capitalize()}"
    }

    protected void calculateDependencies(String descr, String version) {
        // create a temporary configuration to resolve the file
        Configuration conf = project.getConfigurations().detachedConfiguration(project.getDependencies().create("${descr}:${version}"))
        conf.setTransitive(true)
        conf.getResolvedConfiguration().firstLevelModuleDependencies.each { dependency ->
            dependency.children.each { child ->
                String tmpModule = "${child.moduleGroup}:${child.moduleName}".toString()
                String tmpVersion = versions.get(tmpModule)
                if(tmpVersion && tmpVersion != child.moduleVersion) {
                    project.logger.warn('There are two versions for {} - {} and {}', tmpModule, tmpVersion, child.moduleVersion)
                    if(override) {
                        try {
                            Version oldVersion = Version.valueOf(tmpVersion)
                            Version newVersion = Version.valueOf(child.moduleVersion)
                            if(oldVersion < newVersion) {
                                project.logger.quiet('Version {} was replaced with {} for {}:{}', oldVersion, newVersion, child.moduleGroup, child.moduleName)
                                versions.put(tmpModule, child.moduleVersion)
                            }
                        } catch(ParserException pex) {
                            if(tmpVersion < child.moduleVersion) {
                                project.logger.quiet('Version {} was replaced with {} for {}:{}', tmpVersion, child.moduleVersion, child.moduleGroup, child.moduleName)
                                versions.put(tmpModule, child.moduleVersion)
                            }
                        }
                    }
                }
                if(! tmpVersion) {
                    versions.put(tmpModule, child.moduleVersion)
                    project.logger.quiet('Filter for {}:{}:{} from transitive dependencies', child.moduleGroup, child.moduleName, child.moduleVersion)
                }
                calculateDependencies("${child.moduleGroup}:${child.moduleName}".toString(), child.moduleVersion)
            }
        }
    }
}
