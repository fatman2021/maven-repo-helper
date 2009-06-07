/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author ludo
 */
public class Dependency {

    private String groupId;
    private String artifactId;
    private String type;
    private String version;

    public Dependency() {
    }

    public Dependency(String groupId, String artifactId, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Dependency other = (Dependency) obj;
        if ((this.groupId == null) ? (other.groupId != null) : !this.groupId.equals(other.groupId)) {
            return false;
        }
        if ((this.artifactId == null) ? (other.artifactId != null) : !this.artifactId.equals(other.artifactId)) {
            return false;
        }
        if ((this.type == null) ? (other.type != null) : !this.type.equals(other.type)) {
            return false;
        }
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
        hash = 31 * hash + (this.artifactId != null ? this.artifactId.hashCode() : 0);
        hash = 31 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 31 * hash + (this.version != null ? this.version.hashCode() : 0);
        return hash;
    }

    public String toString() {
        return groupId + ":" + artifactId + ":" + type + ":" + version;
    }

    public Dependency applyRules(Collection rules) {
        for (Iterator i = rules.iterator(); i.hasNext();) {
            DependencyRule rule = (DependencyRule) i.next();
            if (rule.matches(this)) {
                return rule.apply(this);
            }
        }
        return this;
    }

    public DependencyRule findMatchingRule(Collection rules) {
        for (Iterator i = rules.iterator(); i.hasNext();) {
            DependencyRule rule = (DependencyRule) i.next();
            if (rule.matches(this)) {
                return rule;
            }
        }
        return null;
    }

    public static List applyRules(List dependencies, Collection rules) {
        if (dependencies == null) {
            return null;
        }
        List result = new ArrayList();
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            result.add(dependency.applyRules(rules));
        }
        return result;
    }

}
