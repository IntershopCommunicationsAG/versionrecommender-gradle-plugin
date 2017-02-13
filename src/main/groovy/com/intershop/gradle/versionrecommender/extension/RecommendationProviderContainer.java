package com.intershop.gradle.versionrecommender.extension;

import com.intershop.gradle.versionrecommender.provider.IvyProvider;
import com.intershop.gradle.versionrecommender.provider.MavenProvider;
import com.intershop.gradle.versionrecommender.provider.PropertiesProvider;
import com.intershop.gradle.versionrecommender.tasks.ResetVersion;
import com.intershop.gradle.versionrecommender.tasks.SetLocalVersion;
import com.intershop.gradle.versionrecommender.tasks.UpdateVersion;
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

        if(provider.isAdaptable()) {
            SetLocalVersion localTask = project.getTasks().create(provider.getTaskName("setLocal"), SetLocalVersion.class);
            localTask.setProvider(provider);
            SetLocalVersion snapshotTask = project.getTasks().create(provider.getTaskName("setSnapshot"), SetLocalVersion.class);
            localTask.setProvider(provider);
            ResetVersion resetTask = project.getTasks().create(provider.getTaskName("reset"), ResetVersion.class);
            resetTask.setProvider(provider);

            UpdateVersion updateTask = project.getTasks().create(provider.getTaskName("update"), UpdateVersion.class);
            updateTask.setProvider(provider);
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
