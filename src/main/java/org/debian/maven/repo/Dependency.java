package org.debian.maven.repo;

/*
 * Copyright 2009 Ludovic Claude.
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
 * limitations under the License.
 */

import java.util.*;

/**
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Dependency implements Comparable, Cloneable {

    private String groupId;
    private String artifactId;
    private String type;
    private String version;
    private boolean optional;
    private String scope;
    private String classifier;

    public Dependency(String groupId, String artifactId, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.scope = "runtime";
        this.classifier = "";
    }

    public Dependency(String groupId, String artifactId, String type, String version, String scope, boolean optional,
                      String classifier) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.scope = scope;
        this.optional = optional;
        this.classifier = (classifier == null) ? "" : classifier;
    }

    public Dependency(Dependency dependency) {
        this.groupId = dependency.groupId;
        this.artifactId = dependency.artifactId;
        this.type = dependency.type;
        this.version = dependency.version;
        this.scope = dependency.scope;
        this.optional = dependency.optional;
        this.classifier = dependency.classifier;
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

    public boolean isOptional() {
        return optional;
    }

    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClassifier() {
        return classifier;
    }

    public void setClassifier(String classifier) {
        this.classifier = (classifier == null) ? "" : classifier;
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
        // Ignore scope and optional as they are content-free and indicate more the context
        if ((this.classifier == null) ? (other.classifier != null) : !this.classifier.equals(other.classifier)) {
            return false;
        }
        return true;
    }

    public boolean equalsIgnoreVersion(Object obj) {
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
        // Classifier is still important here as it can influence greatly the contents of the artifact (a source artifact is very different from a normal artifact)
        if ((this.classifier == null) ? (other.classifier != null) : !this.classifier.equals(other.classifier)) {
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

    public int compareTo(Object o) {
        Dependency dependency = (Dependency) o; 
        if (this.groupId != dependency.groupId) {
            if (this.groupId == null) return -1;
            if (! this.groupId.equals(dependency.groupId)) {
                return this.groupId.compareTo(dependency.groupId);
            }
        }
        if (this.artifactId != dependency.artifactId) {
            if (this.artifactId == null) return -1;
            if (! this.artifactId.equals(dependency.artifactId)) {
                return this.artifactId.compareTo(dependency.artifactId);
            }
        }
        if (this.type != dependency.type) {
            if (this.type == null) return -1;
            if (! this.type.equals(dependency.type)) {
                return this.type.compareTo(dependency.type);
            }
        }
        if (this.version != dependency.version) {
            if (this.version == null) return -1;
            if (! this.version.equals(dependency.version)) {
                return this.version.compareTo(dependency.version);
            }
        }
        return 0;
    }

    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Dependency applyRules(Collection rules) {
        for (Iterator i = rules.iterator(); i.hasNext();) {
            DependencyRule rule = (DependencyRule) i.next();
            if (rule.matches(this)) {
                return rule.apply(this);
            }
        }
        return new Dependency(this);
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

    public static List applyIgnoreRules(List dependencies, Set ignoreRules) {
        if (dependencies == null) {
            return null;
        }
        List result = new ArrayList();
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.findMatchingRule(ignoreRules) == null) {
                result.add(new Dependency(dependency));
            }
        }
        return result;
    }

}
