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

package org.debian.maven.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a Maven dependency of any type (dependency, plugin, parent POM)
 * 
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Dependency implements Comparable<Dependency>, Cloneable {

    public static final Dependency PROTO_JAR = new Dependency(null, null, "jar", null);
    public static final Dependency PROTO_PLUGIN = new Dependency("org.apache.maven.plugins", null, "maven-plugin", null);
    private static final Pattern COMPACT_DEPENDENCY_NOTATION_MATCHER =
            Pattern.compile("(\\w[a-zA-Z0-9\\-_\\.]*):(\\w[a-zA-Z0-9\\-_]*):(\\d[a-zA-Z0-9\\-_\\.]*)");

    private String groupId;
    private String artifactId;
    private String type;
    private String version;
    private boolean optional;
    private String scope;
    private String classifier;
    private String relativePath;
    private boolean superPom;
    private String systemPath;

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
        this.systemPath = dependency.systemPath;
        this.superPom = dependency.superPom;
    }

    public Dependency(Builder builder) {
        this.groupId = builder.groupId;
        this.artifactId = builder.artifactId;
        this.type = builder.type;
        this.version = builder.version;
        this.scope = builder.scope;
        this.optional = builder.optional;
        this.classifier = builder.classifier;
        this.relativePath = builder.relativePath;
        this.superPom = builder.superPom;
    }

    public String getArtifactId() {
        return artifactId;
    }

    @Deprecated
    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    @Deprecated
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getType() {
        return type;
    }

    @Deprecated
    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    @Deprecated
    public void setVersion(String version) {
        this.version = version;
    }

    public boolean isOptional() {
        return optional;
    }

    @Deprecated
    public void setOptional(boolean optional) {
        this.optional = optional;
    }

    public boolean isScopeDefined() {
        return (scope != null);
    }
    
    public String getScope() {
        return scope == null ? "runtime" : scope;
    }

    @Deprecated
    public void setScope(String scope) {
        this.scope = scope;
    }

    public String getClassifier() {
        return classifier;
    }

    @Deprecated
    public void setClassifier(String classifier) {
        this.classifier = (classifier == null) ? "" : classifier;
    }
    
    public String getRelativePath() {
        return relativePath;
    }

    @Deprecated
    public void setRelativePath(String relativePath) {
        this.relativePath = relativePath;
    }

    public String getSystemPath() {
        return systemPath;
    }

    @Deprecated
    public void setSystemPath(String systemPath) {
        this.systemPath = systemPath;
    }

    @Deprecated
    public void setSuperPom(boolean superPom) {
        this.superPom = superPom;
    }

    public boolean isSuperPom() {
        return superPom;
    }

    /**
     * Tells if this artifact is a pom.
     */
    public boolean isPom() {
        return "pom".equals(type);
    }

    /**
     * Tells if this artifact is a jar.
     */
    public boolean isJar() {
        return "jar".equals(type);
    }

    /**
     * Tells if this artifact is a Maven plugin.
     */
    public boolean isPlugin() {
        return "maven-plugin".equals(type);
    }

    /**
     * Tells if this artifact is an OSGi bundle.
     */
    public boolean isBundle() {
        return "bundle".equals(type);
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

    public static Dependency fromCompactNotation(String depNotation) {
        Matcher dependencyMatcher = COMPACT_DEPENDENCY_NOTATION_MATCHER.matcher(depNotation);
        if (dependencyMatcher.matches()) {
            return new Dependency(dependencyMatcher.group(1),
                    dependencyMatcher.group(2), "jar", dependencyMatcher.group(3));
        }
        return null;
    }

    public String formatCompactNotation() {
        return getGroupId() + ":" + getArtifactId() + ":" + getVersion();
    }

    public Builder builder() {
        Builder builder = new Builder();
        builder.groupId = groupId;
        builder.artifactId = artifactId;
        builder.type = type;
        builder.version = version;
        builder.optional = optional;
        builder.scope = scope;
        builder.classifier = classifier;
        builder.relativePath = relativePath;
        builder.superPom = superPom;
        return builder;
    }

    public static class Builder {
        private String groupId;
        private String artifactId;
        private String type;
        private String version;
        private boolean optional;
        private String scope;
        private String classifier;
        private String relativePath;
        private boolean superPom;

        public Builder setGroupId(String groupId) { this.groupId = groupId; return this; }
        public Builder setArtifactId(String artifactId) { this.artifactId = artifactId; return this; }
        public Builder setType(String type) { this.type = type; return this; }
        public Builder setVersion(String version) { this.version = version; return this; }
        public Builder setOptional(boolean optional) { this.optional = optional; return this; }
        public Builder setScope(String scope) { this.scope = scope; return this; }
        public Builder setClassifier(String classifier) { this.classifier = classifier; return this; }
        public Builder setRelativePath(String relativePath) { this.relativePath = relativePath; return this; }
        public Builder setSuperPom(boolean superPom) { this.superPom = superPom; return this; }

        public Builder set(Field field, String value) {
            switch (field) {
                case GROUPID: setGroupId(value); return this;
                case ARTIFACTID: setArtifactId(value); return this;
                case CLASSIFIER: setClassifier(value); return this;
                case OPTIONAL: setOptional("true".equals(value)); return this;
                case RELATIVEPATH: setRelativePath(value); return this;
                case SCOPE: setScope(value); return this;
                case TYPE: setType(value); return this;
                case VERSION: setVersion(value); return this;
            }
            return this;
        }

        public Dependency build() {
            return new Dependency(this);
        }
    }

    public enum Field {
        GROUPID("groupId"),
        ARTIFACTID("artifactId"),
        TYPE("type"),
        VERSION("version"),
        OPTIONAL("optional"),
        SCOPE("scope"),
        CLASSIFIER("classifier"),
        RELATIVEPATH("relativePath");

        public final static Map<String, Field> map;
        static {
            map = new HashMap<String, Field>();
            for(Field field : Field.values()) {
                map.put(field.TAG, field);
            }
        }

        public final String TAG;

        Field(String tag) {
            TAG = tag;
        }

        public static Field get(String tag) {
            return map.get(tag);
        }
    }
}
