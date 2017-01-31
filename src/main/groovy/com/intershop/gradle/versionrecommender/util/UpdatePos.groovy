package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic

/**
 * The names of all digits of a version number, that can be updated.
 */
@CompileStatic
enum UpdatePos {

    MAJOR {
        @Override
        String toString() {
            return 'MAJOR'
        }
    },
    MINOR {
        @Override
        String toString() {
            return 'MINOR'
        }
    },
    PATCH {
        @Override
        String toString() {
            return 'PATCH'
        }
    },
    HOTFIX  {
        @Override
        String toString() {
            return 'HOTFIX'
        }
    },
    NONE {
        @Override
        String toString() {
            return 'NONE'
        }
    }
}