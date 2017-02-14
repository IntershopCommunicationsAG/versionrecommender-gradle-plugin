package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic

@CompileStatic
class NoVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L

    public NoVersionException(String message, Throwable cause) {
        super(message, cause);
    }

    public NoVersionException(String message) {
        super(message);
    }

    public NoVersionException(Throwable cause) {
        super(cause);
    }
}
