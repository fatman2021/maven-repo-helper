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

import java.util.StringTokenizer;

/**
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class DependencyRule implements Comparable<DependencyRule> {

    private static final Rule STAR_RULE = new Rule("*");
    private static final Rule DEBIAN_RULE = new Rule("s/.*/debian/");

    private final Rule groupRule;
    private final Rule artifactRule;
    private final Rule typeRule;
    private final Rule versionRule;
    private final Rule scopeRule;
    private final Rule classifierRule;

    public DependencyRule(String def) {
        StringTokenizer st = new StringTokenizer(def, " \t", false);
        groupRule = maybeParseRule(st);
        artifactRule = maybeParseRule(st);
        typeRule = maybeParseRule(st);
        versionRule = maybeParseRule(st, DEBIAN_RULE);
        classifierRule = maybeParseRule(st);
        scopeRule = maybeParseRule(st);
    }

    public DependencyRule(String group, String artifact, String type, String version, String classifier, String scope) {
        groupRule = new Rule(group);
        artifactRule = new Rule(artifact);
        typeRule = new Rule(type);
        versionRule = new Rule(version);
        classifierRule = new Rule(classifier);
        scopeRule = new Rule(scope);
    }

    public DependencyRule(String group, String artifact, String type, String version) {
        this(group, artifact, type, version, STAR_RULE.toString(), STAR_RULE.toString());
    }

    public static DependencyRule newToMatch(Dependency dependency) {
        return new DependencyRule(
            dependency.getGroupId(),
            dependency.getArtifactId(),
            dependency.getType() == null ? "*" : dependency.getType(),
            "*"
            );
    }

    private static Rule maybeParseRule(StringTokenizer st, Rule defaultRule) {
        if (st.hasMoreTokens()) {
            return new Rule(st.nextToken());
        } else {
            return defaultRule;
        }
    }

    private static Rule maybeParseRule(StringTokenizer st) {
        return maybeParseRule(st, STAR_RULE);
    }

    public Rule getArtifactRule() {
        return artifactRule;
    }

    public Rule getGroupRule() {
        return groupRule;
    }

    public Rule getScopeRule() {
        return scopeRule;
    }

    public Rule getTypeRule() {
        return typeRule;
    }

    public Rule getVersionRule() {
        return versionRule;
    }

    public Rule getClassifierRule() {
        return classifierRule;
    }

    public Rule[] getRules() {
        return new Rule[] { groupRule, artifactRule, typeRule, versionRule, classifierRule, scopeRule };
    }

    public boolean matches(Dependency dependency) {
        return groupRule.match(dependency.getGroupId())
                && artifactRule.match(dependency.getArtifactId())
                && typeRule.match(dependency.getType()) 
                && versionRule.match(dependency.getVersion())
                && scopeRule.match(dependency.getScope())
                && classifierRule.match(dependency.getClassifier());
    }

    public Dependency apply(Dependency dependency) {
        return new Dependency(groupRule.apply(dependency.getGroupId()), 
                artifactRule.apply(dependency.getArtifactId()),
                typeRule.apply(dependency.getType()),
                versionRule.apply(dependency.getVersion()),
                scopeRule.apply(dependency.getScope()),
                dependency.isOptional(),
                classifierRule.apply(dependency.getClassifier()),
                dependency.getRelativePath());
    }

    public boolean matchesAndPreservesGroupArtifactAndType(Dependency dependency) {
        if (!matches(dependency)) {
            return false;
        }
        Dependency transformed = apply(dependency);
        return transformed.getGroupId().equals(dependency.getGroupId())
                && transformed.getArtifactId().equals(dependency.getArtifactId())
                && transformed.getType().equals(dependency.getType());
    }

    public boolean explicitlyMentions(Dependency dependency) {
        return !this.equals(DependencyRuleSet.TO_DEBIAN_VERSION_RULE)
            && !this.equals(DependencyRuleSet.MAVEN_PLUGINS_KEEP_VERSION_RULE)
            && this.matches(dependency);
    }

    /**
     * Natural sort order: from the most specific rules to the most generic rules,
     * a rule is more generic if a generic match appears on the group rule, then
     * artifact rule, then type rule, then version rule.
     * If 2 rules have the same order of genericness, then use an alphabetical
     * sorting of the pattern strings.
     */
    public int compareTo(DependencyRule other) {
        Rule[] rules = getRules();
        Rule[] otherRules = other.getRules();
        
        for (int i = 0; i < rules.length; i++) {
            if (rules[i].isGeneric() && !otherRules[i].isGeneric()) {
                return 1;
            }
            if (!rules[i].isGeneric() &&otherRules[i].isGeneric()) {
                return -1;
            }
        }
        
        // if all rules are generic, rank substitution rules before star rules
        if (groupRule.isGeneric()) {
            for (int i = 0; i < rules.length; i++) {
                if (rules[i].isSubstitution() && !otherRules[i].isSubstitution()) {
                    return -1;
                }
                if (!rules[i].isSubstitution() && otherRules[i].isSubstitution()) {
                    return 1;
                }
            }
        }
        
        return this.toPatternString().compareTo(other.toPatternString());
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final DependencyRule other = (DependencyRule) obj;
        if (this.groupRule != other.groupRule && (this.groupRule == null || !this.groupRule.equals(other.groupRule))) {
            return false;
        }
        if (this.artifactRule != other.artifactRule && (this.artifactRule == null || !this.artifactRule.equals(other.artifactRule))) {
            return false;
        }
        if (this.typeRule != other.typeRule && (this.typeRule == null || !this.typeRule.equals(other.typeRule))) {
            return false;
        }
        if (this.versionRule != other.versionRule && (this.versionRule == null || !this.versionRule.equals(other.versionRule))) {
            return false;
        }
        if (this.classifierRule != other.classifierRule && (this.classifierRule == null || !this.classifierRule.equals(other.classifierRule))) {
            return false;
        }
        return !(this.scopeRule != other.scopeRule && (this.scopeRule == null || !this.scopeRule.equals(other.scopeRule)));
    }

    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.groupRule != null ? this.groupRule.hashCode() : 0);
        hash = 73 * hash + (this.artifactRule != null ? this.artifactRule.hashCode() : 0);
        hash = 73 * hash + (this.typeRule != null ? this.typeRule.hashCode() : 0);
        hash = 73 * hash + (this.versionRule != null ? this.versionRule.hashCode() : 0);
        hash = 73 * hash + (this.classifierRule != null ? this.classifierRule.hashCode() : 0);
        hash = 73 * hash + (this.scopeRule != null ? this.scopeRule.hashCode() : 0);
        return hash;
    }

    public String toPatternString() {
        return groupRule.getPattern() + ":" + artifactRule.getPattern() + ":" + typeRule.getPattern() + ":" + versionRule.getPattern()
                + ":" + classifierRule.getPattern() + ":" + scopeRule.getPattern();
    }

    public String toString() {
        return groupRule + " " + artifactRule + " " + typeRule + " " + versionRule + " " + classifierRule + " " + scopeRule;
    }

}
