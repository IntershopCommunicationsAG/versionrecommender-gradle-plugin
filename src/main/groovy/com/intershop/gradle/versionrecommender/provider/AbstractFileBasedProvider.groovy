package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.update.UpdateConfigurationItem
import com.intershop.gradle.versionrecommender.util.FileInputType
import com.intershop.gradle.versionrecommender.util.VersionExtension
import com.intershop.release.version.ParserException
import com.intershop.release.version.Version
import groovy.transform.CompileStatic
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedArtifact

import java.nio.channels.FileChannel
import java.nio.file.Files
import java.nio.file.Path

@CompileStatic
abstract class AbstractFileBasedProvider extends AbstractVersionProvider {

    protected File inputFile
    protected URL inputURL
    protected URI inputURI
    protected Map inputDependency

    protected FileInputType inputType

    protected VersionExtension versionExtension = VersionExtension.NONE

    boolean transitive
    boolean overrideTransitives

    AbstractFileBasedProvider(final String name, final Project project, final File inputFile) {
        super(name, project)

        if (inputFile == null)
            throw new IllegalArgumentException("Input file may not be null")

        this.inputFile = inputFile
        this.inputType = FileInputType.FILE
    }

    AbstractFileBasedProvider(final String name, final Project project, final Object dependencyNotation) {
        super(name, project)

        if (dependencyNotation == null)
            throw new IllegalArgumentException("Module may not be null")

        if (dependencyNotation && Map.class.isAssignableFrom(dependencyNotation.getClass())) {
            this.inputDependency = (Map) dependencyNotation
        } else {
            this.inputDependency = getDependencyMap("${dependencyNotation}".toString())
        }

        this.inputType = FileInputType.DEPENDENCYMAP
    }

    AbstractFileBasedProvider(final String name, final Project project, final URL inputURL) {
        super(name, project)

        if (inputURL == null)
            throw new IllegalArgumentException("Input URL may not be null")

        this.inputURL = inputURL
        this.inputType = FileInputType.URL
    }

    AbstractFileBasedProvider(final String name, final Project project, final URI inputURI) {
        super(name, project)

        if (inputURI == null)
            throw new IllegalArgumentException("Input URL may not be null")

        this.inputURI = inputURI
        this.inputType = FileInputType.URI
    }

    AbstractFileBasedProvider(final String name, final Project project, final String input, final FileInputType type) {
        super(name, project)
        switch (type) {
            case FileInputType.FILE:
                this.inputFile = new File(input)
                break
            case FileInputType.DEPENDENCYMAP:
                this.inputDependency = getDependencyMap(input)
                break
            case FileInputType.URL:
                inputURL = new URL(input)
                break
            case FileInputType.URI:
                inputURI = URI.create(input)
                break
        }
        this.inputType = type
    }

    abstract String getShortTypeName()

    @Override
    boolean isAdaptable() {
        return (inputType == FileInputType.DEPENDENCYMAP)
    }

    @Override
    void update(UpdateConfiguration updateConfig) {
        if(inputType == FileInputType.DEPENDENCYMAP) {
            UpdateConfigurationItem ucItem = updateConfig.getConfigItem(inputDependency.get('group').toString() ,inputDependency.get('name').toString())
            if(ucItem.getVersion()) {
                writeVersionToFile(ucItem.getVersion(), workingDir)
            } else {
                String updateVersion = updateConfig.getUpdate(inputDependency.get('group').toString(),
                        inputDependency.get('name').toString(),
                        inputDependency.get('version').toString())
                if(updateVersion) {
                    versions = null
                    writeVersionToFile(updateVersion, workingDir)
                }
            }
        } else {
            log.warn('The input type {} is not support for update task.', inputType.toString())
        }
    }

    @Override
    void storeVersionFile() throws IOException {
        if(inputType == FileInputType.DEPENDENCYMAP) {
            String versionStr = null

            File adaptedVersionFile = new File(workingDir, getFileName('version'))
            if(adaptedVersionFile.exists()) {
                versionStr = getVersionFromFile(workingDir)
            }

            if(versionStr) {
                writeVersionToFile(versionStr, workingDir)
            }
        }
    }

    @Override
    void overrideTransitives(boolean override){
        this.overrideTransitives = override
    }

    @Override
    void setVersionExtension(final VersionExtension versionExtension) {
        this.versionExtension = versionExtension

        if (versionExtension != VersionExtension.NONE && inputType == FileInputType.DEPENDENCYMAP) {
            String version = getVersionFromFile(configDir) ?: inputDependency.get('version')

            if (version) {
                version += "-${versionExtension}"
                writeVersionToFile(version, workingDir)
                versions = null
            }
        }
        if (versionExtension == VersionExtension.NONE && inputType == FileInputType.DEPENDENCYMAP) {
            removeVersionFile()
            versions = null
        }
    }

    // protected methods
    protected String getFileName(String fileextension) {
        return ".${getShortTypeName().toLowerCase()}${getName().capitalize()}.${fileextension}"
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
                    log.warn('There are two versions for {} - {} and {}', tmpModule, tmpVersion, child.moduleVersion)
                    if(overrideTransitives) {
                        try {
                            Version oldVersion = Version.valueOf(tmpVersion)
                            Version newVersion = Version.valueOf(child.moduleVersion)
                            if(oldVersion < newVersion) {
                                versions.put(tmpModule, child.moduleVersion)
                            }
                        } catch(ParserException pex) {
                            if(tmpVersion < child.moduleVersion) {
                                versions.put(tmpModule, child.moduleVersion)
                            }
                        }
                    }
                }
                if(! tmpVersion) {
                    versions.put(tmpModule, child.moduleVersion)
                }
                calculateDependencies("${child.moduleGroup}:${child.moduleName}".toString(), child.moduleVersion)
            }
        }
    }

    protected InputStream getStream() {
        InputStream stream = null
        switch (inputType) {
            case FileInputType.FILE:
                stream = getStreamFromFile()
                break
            case FileInputType.DEPENDENCYMAP:
                stream = getStreamFromModule()
                break
            case FileInputType.URL:
                stream = getStreamFromURL()
                break
            case FileInputType.URI:
                stream = getStreamFromURI()
                break
        }
        return stream
    }

    protected File getFile() {
        File file = null
        switch (inputType) {
            case FileInputType.FILE:
                file = inputFile
                break
            case FileInputType.DEPENDENCYMAP:
                file = getFileFromModule()
                break
            case FileInputType.URL:
                file = getTemporaryFile(getStreamFromURL())
                break
            case FileInputType.URI:
                file = getTemporaryFile(getStreamFromURI())
                break
        }
        return file
    }

    private File getTemporaryFile(InputStream input) {
        try {
            Path tempFile = Files.createTempFile(workingDir.toPath(), ".${getShortTypeName().toLowerCase()}${getName().capitalize()}".toString(), 'tmp')
            Files.copy(input, tempFile)
            return tempFile.toFile()
        }catch (IOException ex) {
            log.error('It was not possible to create a temporary file in {} for {} ({}).', workingDir, "${getShortTypeName().toLowerCase()}${getName().capitalize()}".toString(), ex.getMessage())
        }
        return null
    }

    // private methods
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

    private File getFileFromModule() {
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

    private InputStream getStreamFromModule() {
        InputStream rStream
        try {
            rStream = getFileFromModule().newInputStream()
        } catch (Exception ex) {
            log.error('It was not possible to create stream from module input ({}).', ex.getMessage())
            rStream = null
        }
        return rStream
    }

    private InputStream getStreamFromURL() {
        InputStream rStream
        try {
            rStream = inputURL.openStream()
        } catch (Exception ex) {
            log.error('It was not possible to create stream from url input ({}).', ex.getMessage())
            rStream = null
        }
        return rStream
    }

    private InputStream getStreamFromURI() {
        InputStream rStream
        try {
            rStream = inputURI.toURL().openStream()
        } catch (Exception ex) {
            log.error('It was not possible to create stream from url input ({}).', ex.getMessage())
            rStream = null
        }
        return rStream
    }

    private InputStream getStreamFromFile() {
        InputStream rStream
        try {
            rStream = inputFile.newInputStream()
        } catch (Exception ex) {
            log.error('It was not possible to create stream from file input ({}).', ex.getMessage())
            rStream = null
        }
        return rStream
    }

    // extended version handling for temporary changes
    private String getVersionFromFiles() {
        String rVersion = null

        File adaptedVersionFile = new File(workingDir, getFileName('version'))
        if(adaptedVersionFile.exists()) {
            rVersion = getVersionFromFile(workingDir)
        }
        if(! rVersion) {
            rVersion = getVersionFromFile(configDir) ?: inputDependency.get('version')
        }
        return rVersion
    }

    private void writeVersionToFile(String version, File dir) {

        File versionFile = new File(dir, getFileName('version'))
        if(versionFile.exists())
            FileChannel.open(versionFile.toPath()).truncate(0).close()

        versionFile << version
    }

    private void removeVersionFile() {
        File adaptedVersionFile = new File(workingDir, getFileName('version'))
        if(adaptedVersionFile.exists())
            adaptedVersionFile.delete()
    }

    private String getVersionFromFile(File dir) {
        String rVersion = null
        File versionFile = new File(dir, getFileName('version'))
        if(versionFile.exists()) {
            rVersion = versionFile.getText().trim().replaceAll('\\s', '')
        }
        return rVersion
    }


}
