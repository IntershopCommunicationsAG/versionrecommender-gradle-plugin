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
        ModuleVersionIdentifier mvi = ModuleNotationParser.parse(input)

        then:
        thrown IllegalDependencyNotation

        where:
        input << ['org', 'org:name:version:doc:wrong', 'org:name:version@doc@wrong']

    }
}
