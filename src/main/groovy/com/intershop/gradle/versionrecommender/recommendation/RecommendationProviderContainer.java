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
package com.intershop.gradle.versionrecommender.recommendation;

import com.intershop.gradle.versionrecommender.tasks.*;
import groovy.lang.Closure;
import groovy.transform.CompileStatic;
import org.gradle.api.Action;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultNamedDomainObjectList;
import org.gradle.util.ConfigureUtil;

/**
 * This is the container of all recommendation provider configurations.
 * The order of the configuration is important for the version resolution.
 * The main method 'getVersion' resolves the version from the list depending
 * on the module string.
 */
@CompileStatic
public class RecommendationProviderContainer extends DefaultNamedDomainObjectList<RecommendationProvider> {

    private final Project project;

    /**
     * Action helper method for initialization.
     */
    private final Action<? super RecommendationProvider> addLastAction = (Action<RecommendationProvider>) RecommendationProviderContainer.super::add;

    /**
     * Contstructor of this container class
     *
     * @param project   the target project
     */
    public RecommendationProviderContainer(Project project) {
        super(RecommendationProvider.class, null, new RecommendationProviderNamer());
        this.project = project;
    }

    /**
     * Helper class for this container
     */
    private static class RecommendationProviderNamer implements Namer<RecommendationProvider> {
        @Override
        public String determineName(RecommendationProvider r) {
            return r.getName();
        }
    }

    /**
     * This method initializes and configure the recommendation providers. Furthermore
     * it adds this provider to the container. All necessary tasks will be also initialized.
     *
     * @param provider  version recommendation provider
     * @param closure   configuration of the provider
     * @param <T>       implementation of the recommendation provider
     * @return          the configured and initialized provider
     */
    public <T extends RecommendationProvider> T add(T provider, Closure closure) {
        // initialize and configure the recommendation provider
        ConfigureUtil.configure(closure, provider);
        // check, that the name is unique
        assertCanAdd(provider.getName());

        // add provider
        addLastAction.execute(provider);

        // version must be adaptable and the project must be the root project
        if(provider.isAdaptable() && project == project.getRootProject()) {
            // extend version of the provider with LOCAL
            SetLocalVersion localTask = project.getTasks().create(provider.getTaskName("setLocal"), SetLocalVersion.class);
            localTask.setProvider(provider);

            // extend version of the provider with SNAPSHOT
            SetSnapshotVersion snapshotTask = project.getTasks().create(provider.getTaskName("setSnapshot"), SetSnapshotVersion.class);
            snapshotTask.setProvider(provider);

            // reset all temporary version configuration ot the provider
            ResetVersion resetTask = project.getTasks().create(provider.getTaskName("reset"), ResetVersion.class);
            resetTask.setProvider(provider);

            // update the version of the provider (update configuration, if configured is used)
            UpdateVersion updateTask = project.getTasks().create(provider.getTaskName("update"), UpdateVersion.class);
            updateTask.setProvider(provider);

            // set a special version from a special project property
            SetVersion setVersionTask = project.getTasks().create(provider.getTaskName("set"), SetVersion.class);
            setVersionTask.setProvider(provider);

            // store the temporary version information from the
            // temporary file to the real configuration file
            StoreUpdateVersion storeUpdateVersionTask = project.getTasks().create(provider.getTaskName("store"), StoreUpdateVersion.class);
            storeUpdateVersionTask.setProvider(provider);
            storeUpdateVersionTask.setVersionFile(provider.getVersionFile());

            // update task for all configured tasks (default update task list)
            Update defaultUpdateTask = project.getTasks().maybeCreate("update", Update.class);
            defaultUpdateTask.getProviders().add(provider);

            SetAllVersion defaultSetAllVersionTask = project.getTasks().maybeCreate("setVersion", SetAllVersion.class);
            defaultSetAllVersionTask.getProviders().add(provider);

            // store the temporary version of all configured tasks (default update task list)
            StoreUpdate defaultStoreTask = project.getTasks().maybeCreate("store", StoreUpdate.class);
            defaultStoreTask.getProviders().add(provider);
            defaultStoreTask.getVersionFiles().put(provider.getName(), provider.getVersionFile());

            // remove the temporary version file of all configured tasks (default update task list)
            ResetAllVersion resetAll = project.getTasks().maybeCreate("reset", ResetAllVersion.class);
            resetAll.getProviders().add(provider);

            // modify order of tasks
            storeUpdateVersionTask.mustRunAfter(setVersionTask, updateTask);
            resetTask.mustRunAfter(storeUpdateVersionTask, setVersionTask, updateTask, snapshotTask, localTask);

            storeUpdateVersionTask.mustRunAfter(defaultUpdateTask, storeUpdateVersionTask, setVersionTask, updateTask);
            defaultStoreTask.mustRunAfter(defaultUpdateTask, storeUpdateVersionTask, setVersionTask, updateTask);

            resetAll.mustRunAfter(storeUpdateVersionTask, setVersionTask, updateTask, snapshotTask, localTask, defaultUpdateTask, storeUpdateVersionTask);
        }

        return provider;
    }

    /**
     * Ivy descriptor based version recommendation provider.
     *
     * @param name      name of the provider
     * @param input     input object of this provider
     * @param closure   closure with additional ccnfiguration
     * @return          an initialized and configured provider
     */
    IvyRecommendationProvider ivy(String name, Object input, Closure closure) {
        return add(new IvyRecommendationProvider(name, project, input), closure);
    }

    /**
     * Maven BOM file based version recommendation provider.
     *
     * @param name      name of the provider
     * @param input     input object of this provider
     * @param closure   closure with additional ccnfiguration
     * @return          an initialized and configured provider
     */
    MavenRecommendationProvider pom(String name, Object input, Closure closure) {
        return add(new MavenRecommendationProvider(name, project, input), closure);
    }

    /**
     * Properties file based version recommendation provider.
     *
     * @param name      name of the provider
     * @param input     input object of this provider
     * @param closure   closure with additional configuration
     * @return          an initialized and configured provider
     */
    PropertiesRecommendationProvider properties(String name, Object input, Closure closure) {
        return add(new PropertiesRecommendationProvider(name, project, input), closure);
    }

    /**
     * Provider for static properties only.
     *
     * @param name      name of the provider
     * @param closure   configuration
     * @return          an initialized and configured provider
     */
    PropertiesRecommendationProvider properties(String name, Closure closure) {
        return add(new PropertiesRecommendationProvider(name, project), closure);
    }

    /**
     * Returns the version looked up from all providers.
     *
     * @param group     Group or organisation for the version look up
     * @param name      Name or artifact id for the version look up
     * @return          the version string
     */
    public String getVersion(String group, String name) {
        for (int i = 0; i < size(); i++) {
            try {
                String version = get(i).getVersion(group, name);
                if(version != null)
                    return version;
            } catch(Exception e) {
                project.getLogger().error("Exception while polling provider " + get(i).getName() + " for version", e);
            }
        }
        return null;
    }

    public void initializeVersions() {
        for (int i = 0; i < size(); i++) {
            get(i).initializeVersion();
        }
    }
}
