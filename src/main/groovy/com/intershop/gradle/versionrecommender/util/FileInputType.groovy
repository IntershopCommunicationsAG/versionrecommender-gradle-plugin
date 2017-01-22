package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic

/**
 * The names of all digits of a version number, that can be updated.
 */
@CompileStatic
enum FileInputType {

    FILE {
        @Override
        String toString() {
            return 'file'
        }
    },
    URL {
        @Override
        String toString() {
            return 'url'
        }
    },
    URI {
        @Override
        String toString() {
            return 'uri'
        }
    },
    DEPENDENCYMAP  {
        @Override
        String toString() {
            return 'dependencyMap'
        }
    },
    NONE {
        @Override
        String toString() {
            return 'none'
        }
    }
}