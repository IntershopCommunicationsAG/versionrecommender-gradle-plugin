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
package com.intershop.gradle.versionrecommender.provider

import com.intershop.gradle.versionrecommender.util.FileInputType
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

@Slf4j
class MavenProvider extends AbstractFileBasedProvider {

    MavenProvider(final String name, final Project project, final Object input) {
        super(name, project, input)
    }

    @Override
    String getShortTypeName() {
        return 'pom'
    }

    @Override
    synchronized void fillVersionMap() {
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
        ModelBuildingResult result = modelBuilder.build(request)

        result.getEffectiveModel().getDependencyManagement()?.getDependencies().each {Dependency d ->
            if (override || !versions.containsKey("${d.getGroupId()}:${d.getArtifactId()}".toString())) {
                versions.put("${d.getGroupId()}:${d.getArtifactId()}".toString(), d.getVersion())
            }
        }

        result.getEffectiveModel()?.getDependencies().each {Dependency d ->
            if (this.override || !versions.containsKey("${d.getGroupId()}:${d.getArtifactId()}".toString())) {
                versions.put("${d.getGroupId()}:${d.getArtifactId()}".toString(), d.getVersion())
            }
        }
        if(inputType == FileInputType.DEPENDENCYMAP && inputDependency.get('version')) {
            versions.put("${inputDependency.get('group')}:${inputDependency.get('name')}".toString(), inputDependency.get('version').toString())
        }
    }

    @Override
    void setTransitives(boolean transitive) {
        log.warn('Maven BOM provider {} does not support this method (transitive)', name)
    }

    @Override
    void setOverrideTransitives(boolean override){
        log.warn('Maven BOM provider {} does not support this method (overrideTransitives)', name)
    }

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

    private class GradleModelResolver implements ModelResolver {
        private final Project project

        GradleModelResolver(Project project) {
            this.project = project
        }

        @Override
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
