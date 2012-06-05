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
 * Represents a Maven dependency of any type (dependency, plugin, parent POM)
 * 
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Dependency implements Comparable<Dependency>, Cloneable {

    private String groupId;
    private String artifactId;
    private String type;
    private String version;
    private boolean optional;
    private String scope;
    private String classifier;
    private String relativePath;
    private boolean superPom;

    public Dependency(String groupId, String artifactId, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.classifier = "";
    }

    public Dependency(String groupId, String artifactId, String type, String version, String scope, boolean optional,
                      String classifier, String relativePath) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
        this.scope = scope;
        this.optional = optional;
        this.classifier = (classifier == null) ? "" : classifier;
        this.relativePath = relativePath;
    }

    public Dependency(Dependency dependency) {
        this.groupId = dependency.groupId;
        this.artifactId = dependency.artifactId;
        this.type = dependency.type;
        this.version = dependency.version;
        this.scope = dependency.scope;
        this.optional = dependency.optional;
        this.classifier = dependency.classifier;
        this.relativePath = dependency.relativePath;
        this.superPom = dependency.superPom;
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

    public boolean isScopeDefined() {
        return (scope != null);
    }
    
    public String getScope() {
        return scope == null ? "runtime" : scope;
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
    
    public String getRelativePath() {
        return relativePath;
    }

    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public void setSuperPom(boolean superPom) {
        this.superPom = superPom;
    }

    public boolean isSuperPom() {
        return superPom;
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
        if (!isTypeEquals(this.type, other.type)) {
            return false;
        }
        if ((this.version == null) ? (other.version != null) : !this.version.equals(other.version)) {
            return false;
        }
        // Ignore scope and optional as they are content-free and indicate more the context
        return !((this.classifier == null) ? (other.classifier != null) : !this.classifier.equals(other.classifier));
    }

    public boolean equalsIgnoreVersion(Dependency other) {
        if (other == null) {
            return false;
        }
        if (getClass() != other.getClass()) {
            return false;
        }
        if ((this.groupId == null) ? (other.groupId != null) : !this.groupId.equals(other.groupId)) {
            return false;
        }
        if ((this.artifactId == null) ? (other.artifactId != null) : !this.artifactId.equals(other.artifactId)) {
            return false;
        }
        if (!isTypeEquals(this.type, other.type)) {
            return false;
        }
        // Classifier is still important here as it can influence greatly the contents of the artifact (a source artifact is very different from a normal artifact)
        return !((this.classifier == null) ? (other.classifier != null) : !this.classifier.equals(other.classifier));
    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
        hash = 31 * hash + (this.artifactId != null ? this.artifactId.hashCode() : 0);
        // ignore type
        hash = 31 * hash + (this.version != null ? this.version.hashCode() : 0);
        return hash;
    }

    public String toString() {
        return groupId + ":" + artifactId + ":" + type + ":" + version;
    }

    public int compareTo(Dependency dependency) {
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
        // ignore type
        if (this.version != dependency.version) {
            if (this.version == null) return -1;
            if (! this.version.equals(dependency.version)) {
                return this.version.compareTo(dependency.version);
            }
        }
        return 0;
    }

    public Object clone() throws CloneNotSupportedException {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
            return null;
        }
    }

    public Dependency applyRules(Collection<DependencyRule> rules) {
        for (DependencyRule rule: rules) {
            if (rule.matches(this)) {
                return rule.apply(this);
            }
        }
        return new Dependency(this);
    }

    public DependencyRule findMatchingRule(Collection<DependencyRule> rules) {
        for (DependencyRule rule: rules) {
            if (rule.matches(this)) {
                return rule;
            }
        }
        return null;
    }

    public static List<Dependency> applyRules(List<Dependency> dependencies, Collection<DependencyRule> rules) {
        if (dependencies == null) {
            return null;
        }
        List<Dependency> result = new ArrayList<Dependency>();
        for (Dependency dependency: dependencies) {
            result.add(dependency.applyRules(rules));
        }
        return result;
    }

    public static List<Dependency> applyIgnoreRules(List<Dependency> dependencies, Set<DependencyRule> ignoreRules) {
        if (dependencies == null) {
            return null;
        }
        List<Dependency> result = new ArrayList<Dependency>();
        for (Dependency dependency: dependencies) {
            if (dependency.findMatchingRule(ignoreRules) == null) {
                result.add(new Dependency(dependency));
            }
        }
        return result;
    }

    public static boolean isTypeEquals(String type1, String type2) {
        if (type1 != null && ("jar".equals(type1) || "bundle".equals(type1))) {
            return ("jar".equals(type2) || "bundle".equals(type2));
        }
        if (type1 != null) {
            return type1.equals(type2);
        }
        return type2 == null;
    }

}
