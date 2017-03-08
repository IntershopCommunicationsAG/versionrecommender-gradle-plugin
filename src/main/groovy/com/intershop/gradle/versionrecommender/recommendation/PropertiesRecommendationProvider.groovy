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

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.util.FileInputType
import com.intershop.gradle.versionrecommender.util.SimpleVersionProperties
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.regex.Pattern

/**
 * This class implements the access to a properties file.
 */
@Slf4j
@CompileStatic
class PropertiesRecommendationProvider extends FileBasedRecommendationProvider {

    private File propertiesFile

    /**
     * Constructor is called by configur(Closure)
     *
     * @param name      the name of the provider
     * @param project   the target project
     */
    PropertiesRecommendationProvider(final String name, final Project project) {
        super(name, project)
    }

    /**
     * Addditonal constructor with a parameter for the input.
     *
     * @param name      the name of the provider
     * @param project   the target project
     * @param input     input object, can be an File, URL, String or dependency map
     */
    PropertiesRecommendationProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
        changeExcludes = []
    }

    /**
     * List of exceptions for update operation
     * It is possible to add also placeholder.
     */
    List<String> changeExcludes

    /**
     * The config dir is the directory with
     * all stored project files.
     *
     * @param configDir
     */
    @Override
    void setConfigDir(File configDir) {
        if(! isAdaptable()) {
            super.setConfigDir(configDir)
        } else {
            project.logger.warn('Set configuration dir is not supported for {}, because the input is an file ({}],', getName(), inputFile.getName())
        }
    }

    /**
     * The config dir is the directory with
     * all stored project files.
     *
     * @return configuration directory
     */
    @Override
    File getConfigDir() {
        if(isAdaptable()) {
            return inputFile.getParentFile()
        } else {
            return super.getConfigDir()
        }
    }



    /**
     * Returns a type name of an special implementation.
     *
     * @return returns always properties
     */
    @Override
    String getShortTypeName() {
        return 'properties'
    }

    /**
     * If the result of this method is true,
     * it is possible to adapt the version.
     *
     * @return true, if the input is a file in the configuration directory otherwise false
     */
    @Override
    boolean isAdaptable() {
        if(inputType == FileInputType.FILE) {
            if(inputFile != null && inputFile.getParentFile()?.absolutePath?.startsWith(project.projectDir.absolutePath)) {
                return true
            }
        }
        return false
    }

    /**
     * Update the versions of the provider with a
     * special update configuration.
     *
     * @param updateConfig the update configuration for this provider
     */
    @Override
    void update(UpdateConfiguration updateConfig) {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            SimpleVersionProperties svp = getProperties()
            boolean propertiesChanged = false

            svp.keySet().each {
                if(! checkExclude(it.toString())) {
                    String[] groupModul = it.toString().split(':')
                    if(! it.toString().contains('*') && groupModul.size() == 2) {
                        String oldVersion = svp.getProperty(it.toString(), '')
                        String updateVersion = updateConfig.getUpdate(groupModul[0], groupModul.length > 1 ? groupModul[1] : '', oldVersion)

                        if (updateVersion && updateVersion != oldVersion) {
                            svp.setProperty(it.toString(), updateVersion)
                            propertiesChanged = true
                        }
                    } else {
                        log.info('{} is a pattern or an incorrect dependency.', it)
                    }
                }
            }
            if(propertiesChanged) {
                writeVersionProperties(svp, new File(workingDir, inputFile.getName()))
                versions = null
            } else {
                log.warn('No update changes on properties {}', inputFile.getAbsolutePath())
            }
        }
    }

    /**
     * Stores changed version information to
     * the project configuration, if
     * isAdaptable is true.
     *
     * @param Input file for the operation
     * @return File with version information
     * @throws IOException
     */
    @Override
    File store(File outputFile) throws IOException {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {

            File adaptedFile = null

            File workingFile = new File(workingDir, inputFile.getName())
            if(workingFile.exists()) {
                SimpleVersionProperties svp = new SimpleVersionProperties()
                svp.load(workingFile.newInputStream())
                checkVersion(svp)
                adaptedFile = writeVersionProperties(svp, getVersionFile())
            }
            removePropertiesFile()

            return adaptedFile
        }
        return null
    }

    /**
     * Get file object with version information
     *
     * @return file with version information
     */
    @Override
    File getVersionFile() {
        return inputFile
    }

    /**
     * It is possible to add an extension
     * to the existing version, if
     * isAdaptable is true.
     *
     * @param vex version extension, eg SNAPSHOT or LOCAL
     */
    @Override
    void setVersionExtension(final VersionExtension versionExtension) {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            if(versionExtension != VersionExtension.NONE) {
                boolean propertiesChanged = false

                SimpleVersionProperties svp = getProperties()
                svp.keys().each { String key ->
                    if(! checkExclude(key.toString())) {
                        svp.setProperty(key, "${svp.getProperty(key)}-${versionExtension}")
                        propertiesChanged = true
                    }
                }

                if(propertiesChanged) {
                    writeVersionProperties(svp, new File(workingDir, inputFile.getName()))
                    log.info('Versions of {} are extended with {} and written to {}.', getName(), versionExtension.toString(), workingDir.absolutePath)
                    versions = null
                } else {
                    log.warn('No changes on properties {}', inputFile.getAbsolutePath())
                }
            } else {
                removePropertiesFile()
                versions = null
            }
        }
    }

    /**
     * Map with all version information of the provider will
     * be calculated by this method. Before something happens
     * versions is checked for 'null'.
     * The key is a combination of the group or organisation
     * and the name or artifact id. The value is the version.
     */
    @Override
    synchronized void fillVersionMap() {
        InputStream propsStream = getStream()
        if(inputType == FileInputType.FILE && inputFile?.getParentFile() == configDir) {
            File workingFile = new File(workingDir, inputFile.getName())
            if(workingFile.exists()) {
                propsStream = workingFile.newInputStream()
            }
        }
        if (propsStream) {
            log.info('Prepare version list from {} of {}.', getShortTypeName(), getName())

            SimpleVersionProperties svp = new SimpleVersionProperties()
            svp.load(new InputStreamReader(propsStream))
            svp.propertyNames().each { String k ->
                if(k.contains('*')) {
                    globs.put(Pattern.compile(k.trim().replaceAll("\\*", ".*?")), svp.getProperty(k).trim())
                } else {
                    versions.put(k.trim(), svp.getProperty(k).trim())
                }
                if (transitive && !k.contains('*')) {
                    calculateDependencies(k.trim(), svp.getProperty(k).trim())
                }
            }
        }
        if(inputType == FileInputType.DEPENDENCYMAP && inputDependency.get('version')) {
            versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), inputDependency.get('version').toString())
        }
    }

    /**
     * Analysis update exception with placeholdern.
     *
     * @param  module
     * @return true, if the module is included in the list
     */
    private boolean checkExclude(String module) {
        boolean rv = false
        changeExcludes.any { String exc ->
            rv = (module ==~ /${exc.replaceAll("\\*", ".*?")}/)
            return rv
        }
        return rv
    }

    /**
     * Remove a properties file
     */
    private void removePropertiesFile() {
        File adaptedVersionFile = new File(workingDir, inputFile.getName())

        if(adaptedVersionFile.exists()) {
            try {
                log.info('Properties file {} will be removed for {}.', adaptedVersionFile.absolutePath, getName())
                adaptedVersionFile.delete()
                log.info('Properties file {} was removed for {}.', adaptedVersionFile.absolutePath, getName())
            } catch (Exception ex) {
                throw new GradleException("It was not possible to remove file ${adaptedVersionFile.absolutePath} for ${getName()}")
            }
        }
    }

    /**
     * Check version in the property list. An Gradle exception is thrown
     * if the version contains LOCAL.
     * If the extension is SNAPSHOT a warning message will be shown on the console.
     *
     * @param properties object
     */
    private void checkVersion(SimpleVersionProperties svp) {
        svp.values().each {
            if(it.toString().endsWith(VersionExtension.LOCAL.toString())) {
                throw new GradleException("Don't store the LOCAL version for ${getName()} to the project configuration!")
            } else if(it.toString().endsWith(VersionExtension.SNAPSHOT.toString())){
                log.warn('A SNAPSHOT version is stored to the project configuration for {}!', getName())
            }
        }
    }

    /**
     * Write version to a special properties file. In this file
     * only a equals is the separator between key and value. The key
     * can contain a colons.
     *
     * @param props         Properties object.
     * @param versionFile   target file
     * @return              target file if information was written
     */
    private File writeVersionProperties(SimpleVersionProperties props, File versionFile) {
        props.store(versionFile)
        log.info('File {} was written for {}.', versionFile.absolutePath, getName())
        return versionFile
    }

    /**
     * Reads properties list from file.
     * @return a properties object.
     */
    private SimpleVersionProperties getProperties() {
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new InputStreamReader(getStream()))
        return svp
    }


}
