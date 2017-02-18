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
package com.intershop.gradle.versionrecommender.extension;

import com.intershop.gradle.versionrecommender.provider.IvyProvider;
import com.intershop.gradle.versionrecommender.provider.MavenProvider;
import com.intershop.gradle.versionrecommender.provider.PropertiesProvider;
import com.intershop.gradle.versionrecommender.tasks.*;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultNamedDomainObjectList;
import org.gradle.util.ConfigureUtil;

public class RecommendationProviderContainer extends DefaultNamedDomainObjectList<RecommendationProvider> {

    private Project project;

    private final Action<? super RecommendationProvider> addLastAction = new Action<RecommendationProvider>() {
        public void execute(RecommendationProvider r) {
        RecommendationProviderContainer.super.add(r);
        }
    };

    public RecommendationProviderContainer(Project project) {
        super(RecommendationProvider.class, null, new RecommendationProviderNamer());
        this.project = project;
    }

    private static class RecommendationProviderNamer implements Namer<RecommendationProvider> {
        public String determineName(RecommendationProvider r) {
            return r.getName();
        }
    }

    public <T extends RecommendationProvider> T add(T provider, Closure closure) {
        ConfigureUtil.configure(closure, provider);

        assertCanAdd(provider.getName());
        addLastAction.execute(provider);

        if(provider.isAdaptable() && project == project.getRootProject()) {
            SetLocalVersion localTask = project.getTasks().create(provider.getTaskName("setLocal"), SetLocalVersion.class);
            localTask.setProvider(provider);

            SetLocalVersion snapshotTask = project.getTasks().create(provider.getTaskName("setSnapshot"), SetLocalVersion.class);
            snapshotTask.setProvider(provider);

            ResetVersion resetTask = project.getTasks().create(provider.getTaskName("reset"), ResetVersion.class);
            resetTask.setProvider(provider);

            UpdateVersion updateTask = project.getTasks().create(provider.getTaskName("update"), UpdateVersion.class);
            updateTask.setProvider(provider);

            SetVersion setVersionTask = project.getTasks().create(provider.getTaskName("set"), SetVersion.class);
            setVersionTask.setProvider(provider);

            StoreUpdateVersion storeUpdateVersionTask = project.getTasks().create(provider.getTaskName("store"), StoreUpdateVersion.class);
            storeUpdateVersionTask.setProvider(provider);
            storeUpdateVersionTask.setVersionFile(provider.getVersionFile());

            Update defaultUpdateTask = project.getTasks().maybeCreate("update", Update.class);
            defaultUpdateTask.getProviders().add(provider);

            StoreUpdate defaultStoreTask = project.getTasks().maybeCreate("store", StoreUpdate.class);
            defaultStoreTask.getProviders().add(provider);
            defaultStoreTask.getVersionFiles().put(provider.getName(), provider.getVersionFile());

            ResetAllVersion resetAll = project.getTasks().maybeCreate("reset", ResetAllVersion.class);
            resetAll.getProviders().add(provider);
        }

        return provider;
    }

    IvyProvider ivy(String name, Object input, Closure closure) {
        return add(new IvyProvider(name, project, input), closure);
    }

    MavenProvider pom(String name, Object input, Closure closure) {
        return add(new MavenProvider(name, project, input), closure);
    }

    PropertiesProvider properties(String name, Object input, Closure closure) {
        return add(new PropertiesProvider(name, project, input), closure);
    }

    PropertiesProvider properties(String name, Closure closure) {
        return add(new PropertiesProvider(name, project), closure);
    }

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
}
