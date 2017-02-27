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
import com.intershop.gradle.versionrecommender.util.UpdatePos
import com.intershop.gradle.versionrecommender.util.VersionExtension
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

import java.util.regex.Pattern

/**
 * This class implements the main methods and
 * attributes for the configuration of providers.
 */
@CompileStatic
@Slf4j
abstract class RecommendationProvider implements IRecommendationProvider {

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

    /**
     * Constructor is called by configur(Closure)
     *
     * @param name      the name of the provider
     * @param project   the target project
     */
    RecommendationProvider(String name, Project project) {
        this.name = name
        this.project = project

        this.updatePos = UpdatePos.NONE

        setWorkingDir(new File(project.getRootProject().getBuildDir(), VersionRecommenderExtension.EXTENSIONNAME))
        setConfigDir(project.getRootProject().getProjectDir())

        versionMap = [:]
        globs = new HashMap<Pattern, String>()
    }

    /**
     * The name of the proivder.
     *
     * @return name of the provider
     */
    String getName() {
        return this.name
    }

    /**
     * Map with a manual static version configuration.
     * It is not possible to update or change this
     * configuration over tasks.
     */
    Map<String, String> versionMap

    /**
     * The working dir is used for all operations
     * with temporary files.
     *
     * @param workingDir
     */
    @Override
    void setWorkingDir(File workingDir) {
        this.workingDir = workingDir
        if(! workingDir.exists())
            workingDir.mkdirs()
    }

    /**
     * The config dir is the directory with
     * all stored project files.
     *
     * @param configDir
     */
    @Override
    void setConfigDir(File configDir) {
        this.configDir = configDir
        if(! configDir.exists())
            configDir.mkdirs()
    }

    /**
     * The working dir is used for all operations
     * with temporary files.
     *
     * @return working directory
     */
    File getWorkingDir() {
        return this.workingDir
    }

    /**
     * The config dir is the directory with
     * all stored project files.
     *
     * @return configuration directory
     */
    File getConfigDir() {
        return this.configDir
    }

    /**
     * It is possible to add an extension
     * to the existing version, if
     * isAdaptable is true.
     *
     * @param vex version extension, eg SNAPSHOT or LOCAL
     */
    @Override
    void setVersionExtension(VersionExtension versionExtension) {
        this.versionExtension = versionExtension
    }

    /**
     * If the result of this method is true,
     * it is possible to adapt the version
     *
     * @return the default value is always false
     */
    @Override
    boolean isAdaptable() {
        return false
    }

    /**
     * If also transitive dependencies are to be added to the
     * filter list, the value must be set to true.
     *
     * @param transitive true for transitive resolution of configured dependency
     */
    @Override
    void setTransitive(boolean transitive) {
        this.transitive = transitive
    }

    /**
     * If a resolved dependency should override an
     * existing version, the value must be set to true.
     *
     * @param override
     */
    @Override
    void setOverrideTransitiveDeps(boolean override) {
        this.override = override
    }

    /**
     * Map with all version information of the provider will
     * be calculated by this method. Before something happens
     * versions is checked for 'null'.
     * The key is a combination of the group or organisation
     * and the name or artifact id. The value is the version.
     */
    abstract void fillVersionMap()

    /**
     * This method returns true if a version information
     * for this provider is necessary, because it is not
     * part of the stored configuration.
     *
     * @return
     */
    abstract boolean isVersionRequired()

    /**
     * This method returns the name of the
     * project property with a version information.
     *
     * @return
     */
    String getVersionPropertyName() {
        return "${this.getName()}Version"
    }

    /**
     * This method returns the name of a task.
     *
     * @return
     */
    String getTaskName(String prefix) {
        return "${prefix}${getName().capitalize()}"
    }

    /**
     * This method delivers a version from
     * an project property.
     *
     * @return
     */
    String getVersionFromProperty() {
        String versionProperty = ''
        if(project.hasProperty(getVersionPropertyName())) {
            versionProperty = project.property(getVersionPropertyName())
        }
        return versionProperty
    }

    /**
     * Main method of the provider. This method should
     * return a version for an given module with name.
     *
     * @param org Organisiation or Group of the dependency
     * @param name Name or artifact id of the dependency
     * @return the version of the dependency from the provider.
     */
    @Override
    String getVersion(String org, String name) {
        String version = ''
        if(versions == null) {
            versions = [:]

            fillVersionMap()

            if (versionMap) {
                versionMap.each { String k, String v ->
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

    /**
     * This method calculate transitive dependencies and
     * add these configuration to the list. This depends
     * also from setTransitive and setOverrideTransitiveDeps.
     *
     * @param module of the original dependency
     * @param version version of the original dependency
     */
    protected void calculateDependencies(String module, String version) {
        // create a temporary configuration to resolve the file
        Configuration conf = project.getConfigurations().detachedConfiguration(project.getDependencies().create("${module}:${version}"))
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
