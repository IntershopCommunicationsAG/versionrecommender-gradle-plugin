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

@Slf4j
@CompileStatic
class PropertiesProvider extends AbstractFileBasedProvider {

    private File propertiesFile

    PropertiesProvider(final String name, final Project project) {
        super(name, project)
    }

    PropertiesProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
        updateExceptions = []
    }

    @Override
    String getShortTypeName() {
        return 'properties'
    }

    List<String> updateExceptions

    @Override
    boolean isAdaptable() {
        return (inputType == FileInputType.FILE && inputFile.getParentFile() == configDir)
    }

    @Override
    void update(UpdateConfiguration updateConfig) {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            SimpleVersionProperties svp = getProperties()
            boolean propertiesChanged = false

            svp.keySet().each {
                if(! checkUpdateException(it.toString())) {
                    String[] groupModul = it.toString().split(':')
                    String oldVersion = svp.getProperty(it.toString(), '')

                    String updateVersion = updateConfig.getUpdate(groupModul[0], groupModul.length > 1 ? groupModul[1] : '', oldVersion)

                    if (updateVersion && updateVersion != oldVersion) {
                        svp.setProperty(it.toString(), updateVersion)
                        propertiesChanged = true
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

    private boolean checkUpdateException(String orgModule) {
        boolean rv = false
        updateExceptions.any {String exc ->
            rv = (exc ==~ /${exc.replaceAll("\\*", ".*?")}/)
            return rv
        }
        return rv
    }

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

    File getVersionFile() {
        return new File(getConfigDir(), inputFile.getName())
    }

    @Override
    void setVersionExtension(final VersionExtension versionExtension) {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            if(versionExtension != VersionExtension.NONE) {
                SimpleVersionProperties svp = getProperties()
                svp.keys().each { String key ->
                    svp.setProperty(key, "${svp.getProperty(key)}-${versionExtension}")
                }

                println inputFile

                writeVersionProperties(svp, new File(workingDir, inputFile.getName()))
                log.info('Versions of {} are extended with {} and written to {}.', getName(), versionExtension.toString(), workingDir.absolutePath)
                versions = null
            } else {
                removePropertiesFile()
                versions = null
            }
        }
    }

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
                    globs.put(Pattern.compile(k.replaceAll("\\*", ".*?")), svp.getProperty(k))
                } else {
                    versions.put(k, svp.getProperty(k))
                }
                if (transitive && !k.contains('*')) {
                    calculateDependencies(k, svp.getProperty(k))
                }
            }
        }
        if(inputType == FileInputType.DEPENDENCYMAP && inputDependency.get('version')) {
            versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), inputDependency.get('version').toString())
        }
    }

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

    private void checkVersion(SimpleVersionProperties svp) {
        svp.values().each {
            if(it.toString().endsWith(VersionExtension.LOCAL.toString())) {
                throw new GradleException("Don't store the LOCAL version for ${getName()} to the project configuration!")
            } else if(it.toString().endsWith(VersionExtension.SNAPSHOT.toString())){
                log.warn('A SNAPSHOT version is stored to the project configuration for {}!', getName())
            }
        }
    }

    private File writeVersionProperties(SimpleVersionProperties props, File versionFile) {
        props.store(versionFile)
        log.info('File {} was written for {}.', versionFile.absolutePath, getName())
        return versionFile
    }

    private SimpleVersionProperties getProperties() {
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new InputStreamReader(getStream()))
        return svp
    }


}
