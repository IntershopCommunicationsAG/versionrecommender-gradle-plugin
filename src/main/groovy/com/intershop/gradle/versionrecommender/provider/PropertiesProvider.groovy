package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.update.UpdateConfiguration
import com.intershop.gradle.versionrecommender.util.FileInputType
import com.intershop.gradle.versionrecommender.util.SimpleVersionProperties
import com.intershop.gradle.versionrecommender.util.VersionExtension
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.gradle.api.Project

import java.nio.channels.FileChannel
import java.util.regex.Pattern

@Slf4j
@CompileStatic
class PropertiesProvider extends AbstractFileBasedProvider {

    //cache for patterns
    private Map<Pattern, String> globs = null
    private File propertiesFile

    PropertiesProvider(final String name, final Project project, final File inputFile) {
        super(name, project, inputFile)
        versionList = [:]
        updateExceptions = []
    }

    PropertiesProvider(final String name, final Project project, final Object dependencyNotation) {
        super(name, project, dependencyNotation)
        versionList = [:]
        updateExceptions = []
    }

    PropertiesProvider(final String name, final Project project, final URL inputURL) {
        super(name, project, inputURL)
        versionList = [:]
        updateExceptions = []
    }

    PropertiesProvider(final String name, final Project project, final URI inputURI) {
        super(name, project, inputURI)
        versionList = [:]
        updateExceptions = []
    }

    PropertiesProvider(final String name, final Project project, final String input, final FileInputType type) {
        super(name, project, input, type)
        versionList = [:]
        updateExceptions = []
    }

    @Override
    String getShortTypeName() {
        return 'properties'
    }

    Map<String, String> versionList
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
                writeVersionProperties(svp, workingDir)
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
    void storeVersionFile() throws IOException {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            File workingFile = new File(workingDir, inputFile.getName())
            if(workingFile.exists()) {
                SimpleVersionProperties svp = new SimpleVersionProperties()
                svp.load(workingFile.newInputStream())
                writeVersionProperties(svp, configDir)
            }
        }
    }

    @Override
    void setVersionExtension(final VersionExtension versionExtension) {
        if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
            SimpleVersionProperties svp = getProperties()
            svp.keys().each {String key ->
                svp.setProperty(key, "${svp.getProperty(key)}-${versionExtension}")
            }
            writeVersionProperties(svp, workingDir)
            versions = null
        }
    }

    private SimpleVersionProperties getProperties() {
        SimpleVersionProperties svp = new SimpleVersionProperties()
        svp.load(new InputStreamReader(getStream()))
        return svp
    }

    private void writeVersionProperties(SimpleVersionProperties props, File dir) {
        File adaptedVersionFile = new File(dir, inputFile.getName())
        if(adaptedVersionFile.exists())
            FileChannel.open(adaptedVersionFile.toPath()).truncate(0).close()

        props.store(adaptedVersionFile)
    }

    @Override
    void fillVersionMap() {
        if(versions == null) {
            versions = [:]
            InputStream propsStream = getStream()
            if(inputType == FileInputType.FILE && inputFile.getParentFile() == configDir) {
                File workingFile = new File(workingDir, inputFile.getName())
                if(workingFile.exists()) {
                    propsStream = workingFile.newInputStream()
                }
            }
            if (propsStream) {
                SimpleVersionProperties svp = new SimpleVersionProperties()
                svp.load(new InputStreamReader(propsStream))
                svp.propertyNames().each { String k ->
                    versions.put(k, svp.getProperty(k))
                    if (transitive && !k.contains('*')) {
                        calculateDependencies(k, svp.getProperty(k))
                    }
                }
            }
            getVersionList().each { String k, String v ->
                versions.put(k, v)
                if (transitive && !k.contains('*')) {
                    calculateDependencies(k, v)
                }
            }
        }
    }

    @Override
    String getVersion(String org, String name) {
        String ver = super.getVersion(org, name)
        if(ver) {
            return ver
        }

        String key =  "${org}:${name}"
        if(globs == null) {
            globs = new HashMap<Pattern, String>()
            versionList.each { String k, String v ->
                if (k.contains('*')) {
                    globs.put(Pattern.compile(k.replaceAll("\\*", ".*?")), v)
                }
            }
        }

        String rv = null
        globs.any {Pattern p, String gv ->
            if(p.matcher(key).matches()) {
                rv = gv
                return true
            }
        }

        return rv
    }
}
