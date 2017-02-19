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
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

@CompileStatic
@Slf4j
abstract class AbstractFileBasedProvider extends RecommendationProvider {

    protected File inputFile
    protected URL inputURL
    protected Map inputDependency
    protected Object input
    protected boolean versionRequired

    protected FileInputType inputType

    protected VersionExtension versionExtension = VersionExtension.NONE

    AbstractFileBasedProvider(final String name, final Project project)  {
        super(name, project)
        this.input = null
        inputType = FileInputType.NONE
        versionRequired = false
    }

    AbstractFileBasedProvider(final String name, final Project project, final Object input) {
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

    abstract String getShortTypeName()

    @Override
    boolean isAdaptable() {
        return (inputType == FileInputType.DEPENDENCYMAP)
    }

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

    @Override
    void setVersion() throws IOException {
        if(inputType == FileInputType.DEPENDENCYMAP) {
            String propertyVersion = getVersionFromProperty()
            File newVersionFile = new File(getWorkingDir(), getFileName('version'))
            writeVersionToFile(propertyVersion, newVersionFile)
        }
    }

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

    @Override
    File getVersionFile() {
        return new File(getConfigDir(), getFileName('version'));
    }

    private void checkVersion(String version) {
        if(version.endsWith(VersionExtension.LOCAL.toString())) {
            throw new GradleException("Don't store the LOCAL version for ${getName()} to the project configuration!")
        } else if(version.endsWith(VersionExtension.SNAPSHOT.toString())){
            log.warn('A SNAPSHOT version is stored to the project configuration for {}!', getName())
        }
    }

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

    boolean isVersionRequired() {
        if(versionRequired) {
            String rv = getVersionFromFile(getVersionFile())
            if(! rv) {
                return true
            }
        }
        return false
    }

    // protected methods
    protected String getFileName(String fileextension) {
        return ".${getShortTypeName().toLowerCase()}${getName().capitalize()}.${fileextension}"
    }

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
            ex.printStackTrace()
            log.error('It was not possible to create stream from {} input ({}).', input, ex.getMessage())
            stream = null
        }
        return stream
    }

    private static Map getDependencyMap(String depStr) {
        Map returnValue = new HashMap()
        String[] dext = depStr.split('@')
        if (dext.size() > 1) {
            returnValue.put('ext', dext[1])
        }

        String[] mav = dext[0].split(':')
        if (mav.size() < 2) {
            throw new IllegalArgumentException("Group / Org and module name must be specified.")
        }
        returnValue.put('group', mav[0])
        returnValue.put('name', mav[1])

        if (mav.size() > 2) {
            returnValue.put('version', mav[2])
        }
        return returnValue
    }

    protected File getFileFromModule() {
        Map dMap = new HashMap(inputDependency)
        // adapt version
        String version = getVersionFromFiles()
        if(version) {
            dMap.put('version', version)
            // adapt extension
            if (!dMap['ext']) {
                dMap.put('ext', getShortTypeName())
            }

            // create a temporary configuration to resolve the file
            Configuration conf = project.getConfigurations().detachedConfiguration(project.getDependencies().create(dMap))
            ResolvedArtifact artifactId = conf.getResolvedConfiguration().getResolvedArtifacts().iterator().next()
            log.info('Selected recommendation source {}, you requested {}', artifactId?.getId(), dMap)

            return artifactId?.getFile()
        }
        return null
    }

    private String getVersionFromFiles() {
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

    private File writeVersionToFile(String version, File versionFile) {
        versionFile.setText(version)
        log.info('Version {} is stored to {} for {}.', version, versionFile.absolutePath, getName())
        return versionFile
    }

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
