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
