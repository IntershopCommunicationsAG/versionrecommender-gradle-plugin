package com.intershop.gradle.versionrecommender.update

import spock.lang.Specification

class UpdateConfirmationItemSpec extends Specification {

    def 'return sorted list of configItems 1'() {
        when:
        List<UpdateConfigurationItem> items = []
        items.add(new UpdateConfigurationItem('default'))
        items.add(new UpdateConfigurationItem('filter', 'com.intershop.*', ''))
        items.add(new UpdateConfigurationItem('platform', 'com.intershop.platform', ''))
        items.add(new UpdateConfigurationItem('jetty', 'org.jetty', ''))
        List<UpdateConfigurationItem> itemsSorted = items.sort()

        then:
        itemsSorted.collect { it.org } == ['org.jetty', 'com.intershop.platform', 'com.intershop.*', '']
    }

    def 'return sorted list of configItems 2'() {
        when:
        List<UpdateConfigurationItem> items = []
        items.add(new UpdateConfigurationItem('default'))
        items.add(new UpdateConfigurationItem('ccomp', 'com.intershop', 'ccomp'))
        items.add(new UpdateConfigurationItem('bcomp', 'com.intershop', 'bcomp'))
        items.add(new UpdateConfigurationItem('jetty', 'org.jetty', 'jetty'))
        List<UpdateConfigurationItem> itemsSorted = items.sort()

        then:
        itemsSorted.collect { it.module } == ['jetty', 'ccomp', 'bcomp', '']
    }
}
