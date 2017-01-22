package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic

@CompileStatic
enum VersionExtension {
    SNAPSHOT {
        @Override
        String toString() {
            return 'SNAPSHOT'
        }
    },
    LOCAL {
        @Override
        String toString() {
            return 'LOCAL'
        }
    },
    NONE {
        @Override
        String toString() {
            return ''
        }
    }


    static VersionExtension getEnum(String s){
        if(SNAPSHOT.toString().equals(s.trim().toLowerCase())){
            return SNAPSHOT
        }else if(LOCAL.toString().equals(s.trim().toLowerCase())){
            return LOCAL
        }
        return NONE
    }
}