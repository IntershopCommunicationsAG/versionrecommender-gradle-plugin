package com.intershop.gradle.versionrecommender.util

import groovy.transform.CompileStatic

@CompileStatic
class ConfigurationException extends RuntimeException {

    private static final long serialVersionUID = 1L

    public ConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

    public ConfigurationException(String message) {
        super(message);
    }

    public ConfigurationException(Throwable cause) {
        super(cause);
    }
}
