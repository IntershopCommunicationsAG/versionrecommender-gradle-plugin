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
package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic
import org.gradle.api.IllegalDependencyNotation
import org.gradle.api.Project
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier

/**
 * This class provides methods to parse strings or projects for an ModuleVersionIdentifier.
 */
@CompileStatic
class ModuleNotationParser {

    /**
     * Parse a string for ModuleVersionIdentifier
     *
     * @param dependencyNotation
     * @return
     */
    static ModuleVersionIdentifier parse(String dependencyNotation) {
        String[] moduleNotationParts = dependencyNotation.split(":")
        if (moduleNotationParts.length < 2 || moduleNotationParts.length > 4) {
            throw new IllegalDependencyNotation("Supplied String module notation '${dependencyNotation}'" +
                    " is invalid. Example notations: 'org.gradle:gradle-core:2.2', 'org.mockito:mockito-core:1.9.5:javadoc'.")
        }

        String[] moduleParts = moduleNotationParts.last().split('@')
        if(moduleParts.length > 2) {
            throw new IllegalDependencyNotation("Supplied String module notation '${dependencyNotation}'" +
                    " is invalid. Example notations: 'org.gradle:gradle-core:2.2', 'org.mockito:mockito-core:1.9.5@javadoc'.")
        }

        String group = moduleNotationParts[0]

        String name = moduleNotationParts[1]
        if(moduleNotationParts.length == 2 && moduleParts.length == 2) {
            name = moduleParts[0]
        }

        String version = moduleNotationParts.length > 2 ? moduleNotationParts[2] : null
        if(version && moduleParts.length == 2) {
            version = moduleParts[0]
        }

        return [
                getGroup: { group },
                getName: { name },
                getVersion: { version },
                getModule: [ getGroup: group, getName: name ] as ModuleIdentifier
        ] as ModuleVersionIdentifier
    }

    /**
     * Store module information of a project in a ModuleVersionIdentifier
     *
     * @param project
     * @return
     */
    static ModuleVersionIdentifier parse(Project project) {
        ModuleVersionIdentifier mvi = [
            getGroup: { project.getGroup() },
            getName: { project.getName() },
            getVersion: { project.getVersion() },
            getModule: [ getGroup: project.getGroup(), getName: project.getName() ] as ModuleIdentifier
        ] as ModuleVersionIdentifier

        return mvi
    }
}
