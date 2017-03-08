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
import com.intershop.gradle.versionrecommender.update.UpdateConfigurationItem
import com.intershop.gradle.versionrecommender.util.FileInputType
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.GradleException
import org.gradle.api.IllegalDependencyNotation
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * This abstract class implements the most important
 * methods for file based recommendation provider.
 */
@CompileStatic
@Slf4j
abstract class FileBasedRecommendationProvider extends RecommendationProvider {

    protected File inputFile
    protected URL inputURL
    protected Map inputDependency
    protected Object input
    protected boolean versionRequired
    protected FileInputType inputType
    protected VersionExtension versionExtension = VersionExtension.NONE

    /**
     * Constructor is called by configur(Closure)
     *
     * @param name      the name of the provider
     * @param project   the target project
     */
    FileBasedRecommendationProvider(final String name, final Project project)  {
        super(name, project)
        this.input = null
        inputType = FileInputType.NONE
        versionRequired = false
    }

    /**
     * Addditonal constructor with a parameter for the input.
     *
     * @param name      the name of the provider
     * @param project   the target project
     * @param input     input object, can be an File, URL, String or dependency map
     */
    FileBasedRecommendationProvider(final String name, final Project project, final Object input) {
        this(name, project)

        this.input = input
        if(input) {
            switch(input) {
                case File:
                    inputFile = (File) input
                    inputType = FileInputType.FILE
                    break
                case URL:
                    inputURL = (URL) input
                    inputType = FileInputType.URL
                    break
                case String:
                    inputDependency = getDependencyMap("${input}".toString())
                    versionRequired = ! (inputDependency.get('version'))
                    inputType = FileInputType.DEPENDENCYMAP
                    break
                default:
                    if(Map.class.isAssignableFrom(input.getClass())) {
                        inputDependency = (Map) input
                        versionRequired = ! (inputDependency.get('version'))
                        inputType = FileInputType.DEPENDENCYMAP
                    } else {
                        throw new IllegalArgumentException("Input is not a parameter for a provider.")
                    }
                    break
            }
        }
    }

    /**
     * Returns a type name of an special implementation.
     *
     * @return
     */
    abstract String getShortTypeName()

    /**
     * If the result of this method is true,
     * it is possible to adapt the version
     *
     * @return true if the input type is a dependency
     */
    @Override
    boolean isAdaptable() {
        return (inputType == FileInputType.DEPENDENCYMAP)
    }

    /**
     * Update the version of the provider with a
     * special update configuration.
     *
     * @param updateConfig the update configuration for this provider
     */
    @Override
    void update(UpdateConfiguration updateConfig) {
        if(inputType == FileInputType.DEPENDENCYMAP) {
            String g = inputDependency.get('group').toString()
            String n = inputDependency.get('name').toString()
            String v = inputDependency.get('version').toString()

            UpdateConfigurationItem ucItem = updateConfig.getConfigItem(g, n)
            if(ucItem.getVersion()) {
                writeVersionToFile(ucItem.getVersion(), new File(getWorkingDir(), getFileName('version')))
            } else if(getVersionFromProperty()) {
                writeVersionToFile(getVersionFromProperty(), new File(getWorkingDir(), getFileName('version')))
            } else if(v) {
                String updateVersion = updateConfig.getUpdate(g, n, v)
                if(updateVersion) {
                    writeVersionToFile(updateVersion, new File(getWorkingDir(), getFileName('version')))
                    versions = null
                }
            }
        } else {
            log.warn('The input type {} is not support for automatic updates.', inputType.toString())
        }
    }

    /**
     * Overrides the version of the provider, if
     * isAdaptable is true.
     *
     * @throws IOException
     */
    @Override
    void setVersion() throws IOException {
        if(inputType == FileInputType.DEPENDENCYMAP) {
            String propertyVersion = getVersionFromProperty()
            File newVersionFile = new File(getWorkingDir(), getFileName('version'))
            writeVersionToFile(propertyVersion, newVersionFile)
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
        if(inputType == FileInputType.DEPENDENCYMAP) {
            String versionStr = getVersionFromFile(new File(getWorkingDir(), getFileName('version')))
            String propertyVersion = getVersionFromProperty()

            File versionFile = null

            if(propertyVersion) {
                checkVersion(propertyVersion)
                versionFile = writeVersionToFile(propertyVersion, outputFile)
            } else if(versionStr) {
                checkVersion(versionStr)
                versionFile = writeVersionToFile(versionStr, outputFile)
            }

            removeVersionFile()

            return versionFile
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
        return new File(getConfigDir(), getFileName('version'));
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
        this.versionExtension = versionExtension

        if (versionExtension != VersionExtension.NONE && inputType == FileInputType.DEPENDENCYMAP) {
            String version = getVersionFromProperty() ?: getVersionFromFile(getVersionFile())
            version =  version ?: inputDependency.get('version')

            if (version) {
                version += "-${versionExtension}"
                writeVersionToFile(version, new File(getWorkingDir(), getFileName('version')))
                versions = null
            } else {
                throw new GradleException("There is no version for ${inputDependency.get('group')}:${inputDependency.get('name')} specified. Please check your version recommender configuration.")
            }
        }
        if (versionExtension == VersionExtension.NONE && inputType == FileInputType.DEPENDENCYMAP) {
            removeVersionFile()
            versions = null
        }
    }

    /**
     * This method returns true if a version information
     * for this provider is necessary, because it is not
     * part of the stored configuration.
     *
     * @return
     */
    @Override
    boolean isVersionRequired() {
        if(versionRequired) {
            String rv = getVersionFromFile(getVersionFile())
            if(! rv) {
                return true
            }
        }
        return false
    }

    /**
     * Creates a stream from input object.
     *
     * @return
     */
    protected InputStream getStream() {
        InputStream stream = null
        try {
            switch (inputType) {
                case FileInputType.FILE:
                    stream = inputFile.newInputStream()
                    break
                case FileInputType.DEPENDENCYMAP:
                    stream = getFileFromModule()?.newInputStream()
                    break
                case FileInputType.URL:
                    stream = inputURL.openStream()
                    break
            }
        } catch (Exception ex) {
            project.logger.error('It was not possible to create stream from {} input ({}).', input, ex.getMessage())
            stream = null
        }
        return stream
    }

    /**
     * Creates a file from a dependency.
     *
     * @return
     */
    protected File getFileFromModule() {
        // adapt version
        String version = getVersionFromConfig()
        if(version) {
            inputDependency.put('version', version)
            // adapt extension
            if (!inputDependency['ext']) {
                inputDependency.put('ext', getShortTypeName())
            }

            // create a temporary configuration to resolve the file
            Configuration conf = project.getConfigurations().detachedConfiguration(project.getDependencies().create(inputDependency))
            ResolvedArtifact artifactId = conf.getResolvedConfiguration().getResolvedArtifacts().iterator().next()
            log.info('Selected recommendation source {}, you requested {}', artifactId?.getId(), inputDependency)

            return artifactId?.getFile()
        }
        return null
    }

    /**
     * Checks a version for extensions. An Gradle exception is thrown
     * if the version contains LOCAL.
     * If the extension is SNAPSHOT a warning message will be shown on the console.
     *
     * @param version string with version information
     */
    private void checkVersion(String version) {
        if(version.endsWith(VersionExtension.LOCAL.toString())) {
            throw new GradleException("Don't store the LOCAL version for ${getName()} to the project configuration!")
        } else if(version.endsWith(VersionExtension.SNAPSHOT.toString())){
            project.logger.warn('A SNAPSHOT version is stored to the project configuration for {}!', getName())
        }
    }

    /**
     * Calculates a dependency map from string.
     * If the string is not a correct dependency description
     * an IllegalDependencyNotation will be thrown.
     *
     * @param dependencyStr dependency description
     * @return this is a map object with an dependency description.
     */
    private static Map getDependencyMap(String dependencyStr) {
        Map returnValue = new HashMap()
        String[] dext = dependencyStr.split('@')
        if (dext.size() > 1) {
            returnValue.put('ext', dext[1])
        }

        String[] mav = dext[0].split(':')
        if (mav.size() < 2) {
            throw new IllegalDependencyNotation("Supplied String module notation '${dependencyStr}'" +
                    " is invalid. Example notations: 'org.gradle:gradle-core:2.2', 'org.mockito:mockito-core:1.9.5:javadoc'.")
        }
        returnValue.put('group', mav[0])
        returnValue.put('name', mav[1])

        if (mav.size() > 2) {
            returnValue.put('version', mav[2])
        }
        return returnValue
    }

    /**
     * Calculate a filename from provider attributes.
     *
     * @param fileextension extension of a file, eg. properties or version
     * @return complete string
     */
    private String getFileName(String fileextension) {
        return ".${getShortTypeName().toLowerCase()}${getName().capitalize()}.${fileextension}"
    }

    /**
     * Reads a version from a version file.
     * It the file is not available, it tries to use version from
     * property or the specified version or the configured dependency.
     *
     * @return a version string, if no version was found null.
     */
    private String getVersionFromConfig() {
        String rVersion = null

        File adaptedVersionFile = new File(workingDir, getFileName('version'))
        if(adaptedVersionFile.exists()) {
            rVersion = getVersionFromFile(adaptedVersionFile)
        }
        if(! rVersion) {
            rVersion = getVersionFromFile(getVersionFile()) ?: inputDependency.get('version')
        }
        return rVersion
    }

    /**
     * Writes a version string to a file.
     * It returns the file object if the file was written.
     *
     * @param version       the version information
     * @param versionFile   the target file
     * @return              the target file if written
     */
    private File writeVersionToFile(String version, File versionFile) {
        versionFile.setText(version)
        log.info('Version {} is stored to {} for {}.', version, versionFile.absolutePath, getName())
        return versionFile
    }

    /**
     * Removes a version file from the working directory.
     */
    private void removeVersionFile() {
        File adaptedVersionFile = new File(workingDir, getFileName('version'))

        if(adaptedVersionFile.exists()) {
            try {
                log.info('Version file {} will be removed for {}.', adaptedVersionFile.absolutePath, getName())
                adaptedVersionFile.delete()
                log.info('Version file {} was removed for {}.', adaptedVersionFile.absolutePath, getName())
            } catch (Exception ex) {
                throw new GradleException("It was not possible to remove file ${adaptedVersionFile.absolutePath} for ${getName()}")
            }
        }
    }

    /**
     * Return version information from a file.
     *
     * @param versionFile
     * @return version information
     */
    private String getVersionFromFile(File versionFile) {
        if(! versionFile.getParentFile().exists()) {
            versionFile.getParentFile().mkdirs()
        }
        String rVersion = null
        if(versionFile.exists()) {
            rVersion = versionFile.getText().trim().replaceAll('\\s', '')
        }
        return rVersion
    }
}
