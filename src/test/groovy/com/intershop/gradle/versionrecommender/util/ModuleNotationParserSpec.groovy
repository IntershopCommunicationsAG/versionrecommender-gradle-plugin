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

import org.gradle.api.IllegalDependencyNotation
import org.gradle.api.artifacts.ModuleVersionIdentifier
import spock.lang.Specification

class ModuleNotationParserSpec extends Specification {

    def 'test module notation parser with correct values'() {
        when:
        ModuleVersionIdentifier mvi = ModuleNotationParser.parse(input)

        then:
        mvi.group == org
        mvi.name == name
        mvi.version == version

        where:
        input                       | org   | name   | version
        'org:name:version'          | 'org' | 'name' | 'version'
        'org:name'                  | 'org' | 'name' | null
        'org:name:version:javadoc'  | 'org' | 'name' | 'version'
        'org:name:version@javadoc'  | 'org' | 'name' | 'version'
        'org:name@javadoc'          | 'org' | 'name' | null
    }

    def 'test module notation parser with incorrect values'() {
        when:
        ModuleNotationParser.parse(input)

        then:
        thrown IllegalDependencyNotation

        where:
        input << ['org', 'org:name:version:doc:wrong', 'org:name:version@doc@wrong']

    }
}
