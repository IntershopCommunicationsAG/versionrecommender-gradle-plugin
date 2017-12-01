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

import com.intershop.gradle.versionrecommender.util.UpdatePos
import groovy.transform.CompileStatic
import org.gradle.api.Named

/**
 * <p>Update configuration item</p>
 * <p>This item provides all parameter for
 * an single update configuration</p>
 */
@CompileStatic
class UpdateConfigurationItem implements Comparable, Named {

    /**
     * Simple constructor
     *
     * @param name
     */
    UpdateConfigurationItem(String name) {
        this.name = name
        this.org = ''
        this.module = ''
    }

    /**
     * Constructor with basic configuration attributes
     *
     * @param name      Name of the configuration
     * @param org       Organisation
     * @param module    Module name
     */
    UpdateConfigurationItem(String name, String org, String module) {
        this.name = name
        this.org = org
        this.module = module
    }

    /**
     * Name of the configuration item
     */
    String name = ''

    /**
     * Module name
     */
    String module = ''

    /**
     * Organisation name
     */
    String org = ''

    /**
     * Version for this configuration.
     * This is used for the udpate if specified.
     */
    String version = ''

    /**
     * Update position for this special
     * update configuration.
     */
    String update = UpdatePos.HOTFIX.toString()

    /**
     * Returns an update position objec from
     * update attribute.
     *
     * @return
     */
    UpdatePos getUpdatePos() {
        return update as UpdatePos
    }

    /**
     * Search pattern for special versions in a list
     */
    String searchPattern = ''

    /**
     * Search pattern for the configured version.
     * The default value is the search pattern.
     */
    String versionPattern

    String getVersionPattern() {
        if(versionPattern) {
            return versionPattern
        }
        if(searchPattern) {
            return searchPattern
        } else if(patternForNextVersion) {
            return patternForNextVersion
        }
    }

    /**
     * Complex search pattern for the next version.
     */
    String patternForNextVersion = ''

    /**
     * Number of the pattern group for updates
     */
    int sortStringPos = 0

    /**
     * Equals method for update configuration items
     *
     * @param other
     * @return
     */
    @Override
    boolean equals(Object other) {
        if (this == other) {
            return true
        }
        if (!(other instanceof UpdateConfigurationItem)) {
            return false
        }
        return compareTo((UpdateConfigurationItem) other) == 0
    }

    /**
     * Compare method for update configuration items
     *
     * @param other
     * @return
     */
    @Override
    int compareTo(Object other) {
        if(((UpdateConfigurationItem)other).org == this.org) {
            if(this.module != ((UpdateConfigurationItem)other).module) {
                if(((UpdateConfigurationItem)other).module) {
                    return ((UpdateConfigurationItem)other).module.compareTo(this.module)
                } else {
                    return -1 * this.module.compareTo(((UpdateConfigurationItem)other).module)
                }
            } else {
                return 0
            }
        } else {
            if(((UpdateConfigurationItem)other).org) {
                return ((UpdateConfigurationItem)other).org.compareTo(this.org)
            } else {
                return -1 * this.org.compareTo(((UpdateConfigurationItem)other).org)
            }
        }
    }
}
