package com.intershop.gradle.versionrecommender.extension;

import com.intershop.gradle.versionrecommender.provider.IvyProvider;
import com.intershop.gradle.versionrecommender.provider.MavenProvider;
import com.intershop.gradle.versionrecommender.provider.PropertiesProvider;
import com.intershop.gradle.versionrecommender.util.FileInputType;
import groovy.lang.Closure;
import org.gradle.api.Action;
import org.gradle.api.Namer;
import org.gradle.api.Project;
import org.gradle.api.internal.DefaultNamedDomainObjectList;

import java.io.File;
import java.net.URL;
import java.util.Map;

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
        project.configure(provider, closure);
        assertCanAdd(provider.getName());
        addLastAction.execute(provider);

        System.out.println(provider.getName());
        project.getTasks().create( provider.getName() );
        return provider;
    }

    RecommendationProvider ivy(String name, File file, Closure closure) {
        return add(new RecommendationProvider(name, new IvyProvider(name, project, file)), closure);
    }

    RecommendationProvider ivy(String name, URL url, Closure closure) {
        return add(new RecommendationProvider(name, new IvyProvider(name, project, url)), closure);
    }

    RecommendationProvider ivy(String name, String dependency, Closure closure) {
        return add(new RecommendationProvider(name, new IvyProvider(name, project, dependency, FileInputType.DEPENDENCYMAP)), closure);
    }

    RecommendationProvider ivy(String name, Map dependency, Closure closure) {
        return add(new RecommendationProvider(name, new IvyProvider(name, project, dependency)), closure);
    }

    RecommendationProvider mavenBom(String name, File file, Closure closure) {
        return add(new RecommendationProvider(name, new MavenProvider(name, project, file)), closure);
    }

    RecommendationProvider mavenBom(String name, URL url, Closure closure) {
        return add(new RecommendationProvider(name, new MavenProvider(name, project, url)), closure);
    }

    RecommendationProvider mavenBom(String name, String dependency, Closure closure) {
        return add(new RecommendationProvider(name, new MavenProvider(name, project, dependency, FileInputType.DEPENDENCYMAP)), closure);
    }

    RecommendationProvider mavenBom(String name, Map dependency, Closure closure) {
        return add(new RecommendationProvider(name, new MavenProvider(name, project, dependency)), closure);
    }

    RecommendationProvider properties(String name, File file, Closure closure) {
        return add(new RecommendationProvider(name, new PropertiesProvider(name, project, file)), closure);
    }

    RecommendationProvider properties(String name, URL url, Closure closure) {
        return add(new RecommendationProvider(name, new PropertiesProvider(name, project, url)), closure);
    }

    RecommendationProvider properties(String name, String dependency, Closure closure) {
        return add(new RecommendationProvider(name, new PropertiesProvider(name, project, dependency, FileInputType.DEPENDENCYMAP)), closure);
    }

    RecommendationProvider properties(String name, Map dependency, Closure closure) {
        return add(new RecommendationProvider(name, new PropertiesProvider(name, project, dependency)), closure);
    }

    RecommendationProvider properties(String name, Closure closure) {
        return add(new RecommendationProvider(name, new PropertiesProvider(name, project, null, FileInputType.FILE)), closure);
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
