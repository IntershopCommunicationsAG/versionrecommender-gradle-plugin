package com.intershop.gradle.versionrecommender.update

import spock.lang.Specification

class UpdateConfirmationItemSpec extends Specification {

    def 'return sorted list of configItems 1'() {
        when:
        List<UpdateConfigurationItem> items = []
        items.add(new UpdateConfigurationItem())
        items.add(new UpdateConfigurationItem('com.intershop.*', ''))
        items.add(new UpdateConfigurationItem('com.intershop.platform', ''))
        items.add(new UpdateConfigurationItem('org.jetty', ''))
        List<UpdateConfigurationItem> itemsSorted = items.sort()

        then:
        itemsSorted.collect { it.org } == ['org.jetty', 'com.intershop.platform', 'com.intershop.*', '']
    }

    def 'return sorted list of configItems 2'() {
        when:
        List<UpdateConfigurationItem> items = []
        items.add(new UpdateConfigurationItem())
        items.add(new UpdateConfigurationItem('com.intershop', 'ccomp'))
        items.add(new UpdateConfigurationItem('com.intershop', 'bcomp'))
        items.add(new UpdateConfigurationItem('org.jetty', 'jetty'))
        List<UpdateConfigurationItem> itemsSorted = items.sort()

        then:
        itemsSorted.collect { it.module } == ['jetty', 'ccomp', 'bcomp', '']
    }
}
