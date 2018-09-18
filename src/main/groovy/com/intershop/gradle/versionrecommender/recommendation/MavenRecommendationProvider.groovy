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

import com.intershop.gradle.versionrecommender.util.FileInputType
import groovy.transform.CompileStatic
import groovy.transform.TypeChecked
import groovy.transform.TypeCheckingMode
import groovy.util.logging.Slf4j
import org.apache.maven.model.Dependency
import org.apache.maven.model.Model
import org.apache.maven.model.Parent
import org.apache.maven.model.Repository
import org.apache.maven.model.building.*
import org.apache.maven.model.interpolation.StringSearchModelInterpolator
import org.apache.maven.model.path.DefaultPathTranslator
import org.apache.maven.model.path.DefaultUrlNormalizer
import org.apache.maven.model.resolution.InvalidRepositoryException
import org.apache.maven.model.resolution.ModelResolver
import org.apache.maven.model.resolution.UnresolvableModelException
import org.codehaus.plexus.interpolation.MapBasedValueSource
import org.codehaus.plexus.interpolation.PropertiesBasedValueSource
import org.codehaus.plexus.interpolation.ValueSource
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.ArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository

import java.nio.file.Files
import java.nio.file.Path

/**
 * This class implements the access to an Maven BOM file.
 */
@CompileStatic
@Slf4j
class MavenRecommendationProvider extends FileBasedRecommendationProvider {

    /**
     * Constructor is called by configur(Closure)
     *
     * @param name      the name of the provider
     * @param project   the target project
     */
    MavenRecommendationProvider(final String name, final Project project)  {
        super(name, project)
    }

    /**
     * Addditonal constructor with a parameter for the input.
     *
     * @param name      the name of the provider
     * @param project   the target project
     * @param input     input object, can be an File, URL, String or dependency map
     */
    MavenRecommendationProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
    }

    /**
     * Returns a type name of an special implementation.
     *
     * @return returns always pom
     */
    @Override
    String getShortTypeName() {
        return 'pom'
    }

    /**
     * Map with all version information of the provider will
     * be calculated by this method. Before something happens
     * versions is checked for 'null'.
     * The key is a combination of the group or organisation
     * and the name or artifact id. The value is the version.
     */
    @Override
    void fillVersionMap() {
        log.info('Prepare version list from {} of {}.', getShortTypeName(), getName())

        DefaultModelBuildingRequest request = new DefaultModelBuildingRequest()
        request.setSystemProperties(System.getProperties())

        Properties props = new Properties()
        project.getProperties().entrySet().each {
            if(it.key && it.value) {
                props.put(it.key, it.value)
            }
        }

        request.setUserProperties(props)

        request.setPomFile(this.getFile())
        request.setModelResolver(new GradleModelResolver(this.project))

        DefaultModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance()

        modelBuilder.setModelInterpolator(new ProjectPropertiesModelInterpolator(project))
        try {
            ModelBuildingResult result = modelBuilder.build(request)

            if (result.getEffectiveModel().getDependencyManagement() && result.getEffectiveModel().getDependencyManagement().getDependencies()) {
                result.getEffectiveModel().getDependencyManagement().getDependencies().each { Dependency d ->
                    if (override || !versions.containsKey("${d.getGroupId()}:${d.getArtifactId()}".toString())) {
                        versions.put("${d.getGroupId()}:${d.getArtifactId()}".toString(), d.getVersion())
                    }
                }
            }
            if (result.getEffectiveModel().getDependencies()) {
                result.getEffectiveModel().getDependencies().each { Dependency d ->
                    if (this.override || !versions.containsKey("${d.getGroupId()}:${d.getArtifactId()}".toString())) {
                        versions.put("${d.getGroupId()}:${d.getArtifactId()}".toString(), d.getVersion())
                    }
                }
            }
            if (inputType == FileInputType.DEPENDENCYMAP && inputDependency.get('version')) {
                versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), super.getVersionFromConfig())
            }
        } catch(NullPointerException npe) {
            log.error('It is not possible to resolve the filter {} of {}.', getShortTypeName(), getName())
        }
    }

    /**
     * It is not possible to change this behavior. Dependencies will
     * be always calculated transitive.
     * Therefore this method is not supported for this provider. A warning
     * message will be shown in the console.
     *
     * @param transitive
     */
    @Override
    void setTransitive(boolean transitive) {
        log.warn('Maven BOM provider {} does not support this method (transitive)', name)
    }

    /**
     * It is not possible to change this behavior. Dependencies will
     * be always calculated transitive.
     * Therefore this method is not supported for this provider. A warning
     * message will be shown in the console.
     *
     * @param transitive
     */
    @Override
    void setOverrideTransitiveDeps(boolean override){
        log.warn('Maven BOM provider {} does not support this method (overrideTransitives)', name)
    }

    /**
     * Helper class for the pom file analysis.
     */
    private static class ProjectPropertiesModelInterpolator extends StringSearchModelInterpolator {
        private final Project project

        ProjectPropertiesModelInterpolator(Project project) {
            this.project = project
            setUrlNormalizer(new DefaultUrlNormalizer())
            setPathTranslator(new DefaultPathTranslator())
        }

        List<ValueSource> createValueSources(Model model, File projectDir, ModelBuildingRequest request, ModelProblemCollector collector) {
            List<ValueSource> sources = new ArrayList<>()
            sources.addAll(super.createValueSources(model, projectDir, request, collector))
            sources.add(new PropertiesBasedValueSource(System.getProperties()))
            sources.add(new MapBasedValueSource(project.getProperties()))
            return sources
        }
    }

    /**
     * Helper class for the pom file analysis.
     */
    private class GradleModelResolver implements ModelResolver {
        private final Project project

        GradleModelResolver(Project project) {
            this.project = project
        }

        @Override
        @TypeChecked(TypeCheckingMode.SKIP)
        ModelSource resolveModel(String groupId, String artifactId, String version) throws UnresolvableModelException {

            ModelSource sms = null
            String relativeURL = groupId.replaceAll('\\.','/')
            relativeURL += "/${artifactId}/${version}/${artifactId}-${version}.pom"

            try {
                List<ArtifactRepository> repList = project.getRepositories().findAll {it instanceof  MavenArtifactRepository}

                repList.any {ArtifactRepository repo ->
                    URL url = new URL(((MavenArtifactRepository) repo).getUrl().toString() + "/" + relativeURL)

                    try {
                        sms = new StreamModelSource(url.openStream())
                        return true
                    } catch (IOException ioex) {
                        sms = null
                        log.warn('It was not possible to resolve URL {} ({})', url, ioex.getMessage())
                    }
                }
            } catch (MalformedURLException malurlex) {
                throw new RuntimeException(malurlex)
            }

            return sms
        }

        @SuppressWarnings( "deprecation" )
        @Override
        ModelSource resolveModel(Parent parent) throws UnresolvableModelException {
            return this.resolveModel(parent.groupId, parent.getArtifactId(), parent.getVersion())
        }

        @Override
        void addRepository(Repository repository) throws InvalidRepositoryException {}

        @Override
        void addRepository(Repository repository, boolean replace) throws InvalidRepositoryException {}

        @Override
        ModelResolver newCopy() {
            return this
        }
    }

    /**
     * Helper class for the pom file analysis.
     */
    private class StreamModelSource implements ModelSource {
        InputStream inputStream

        StreamModelSource(InputStream inputStream) {
            this.inputStream = inputStream
        }

        @Override
        InputStream getInputStream() throws IOException {
            return inputStream
        }

        @Override
        String getLocation() {
            return null
        }
    }

    /**
     * The analyzer for the Maven file needs a local
     * file.
     * @return
     */
    private File getFile() {
        File file = null
        try {
            switch (inputType) {
                case FileInputType.FILE:
                    file = inputFile
                    break
                case FileInputType.DEPENDENCYMAP:
                    file = getFileFromModule()
                    break
                case FileInputType.URL:
                    file = getTemporaryFile(inputURL.openStream())
                    break
            }
        } catch (Exception ex) {
            log.error('It was not possible to create file from {} input ({}).', input, ex.getMessage())
            file = null
        }
        return file
    }

    /**
     * The analyzer for the Maven file needs a local
     * file, therefore the file is stored temporary from an URL.
     * @return a file object for analysis
     */
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
}
