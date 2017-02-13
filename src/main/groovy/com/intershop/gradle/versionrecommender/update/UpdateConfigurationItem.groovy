package com.intershop.gradle.versionrecommender.update

import com.intershop.gradle.versionrecommender.util.UpdatePos
import org.gradle.api.Named

class UpdateConfigurationItem implements Comparable, Named {

    UpdateConfigurationItem(String name) {
        this.name = name
        this.org = ''
        this.module = ''
    }

    UpdateConfigurationItem(String name, String org, String module) {
        this.name = name
        this.org = org
        this.module = module
    }

    String name

    String module = ''

    String org = ''

    String version = ''

    UpdatePos updatePos = UpdatePos.HOTFIX

    String searchPattern = ''

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

    String patternForNextVersion = ''

    int sortStringPos = 0

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UpdateConfigurationItem)) {
            return false;
        }
        return compareTo((UpdateConfigurationItem) other) == 0
    }

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
            if(other.org) {
                return ((UpdateConfigurationItem)other).org.compareTo(this.org)
            } else {
                return -1 * this.org.compareTo(((UpdateConfigurationItem)other).org)
            }
        }
    }
}
