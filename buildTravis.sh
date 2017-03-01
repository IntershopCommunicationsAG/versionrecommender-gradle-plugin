#!/bin/bash
# This script will build the project.

export JAVA_OPTS="-Xms64m -Xmx96m -XX:MaxPermSize=32m -XX:+CMSClassUnloadingEnabled"
// export GRADLE_OPTS="-Dorg.gradle.daemon=false"

if [ "$TRAVIS_PULL_REQUEST" != "false" ]; then
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        ./gradlew test build -s -Dorg.gradle.testkit.debug=true
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] Tag ['$TRAVIS_TAG']'
        ./gradlew -PrunOnCI=true test build :bintrayUpload :publishPlugins -s -Dorg.gradle.testkit.debug=true
    fi
else
    if [ "$TRAVIS_TAG" == "" ]; then
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] and without Tag'
        ./gradlew test build -s -Dorg.gradle.testkit.debug=true
    else
        echo -e 'Build Branch for Release => Branch ['$TRAVIS_BRANCH'] Tag ['$TRAVIS_TAG']'
        ./gradlew -PrunOnCI=true test build :bintrayUpload :publishPlugins -s -Dorg.gradle.testkit.debug=true
    fi
fi