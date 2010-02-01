package org.debian.maven.repo;

import java.util.StringTokenizer;

/**
 *
 * @author ludo
 */
public class DependencyRule implements Comparable {

    public static DependencyRule TO_DEBIAN_VERSION_RULE = new DependencyRule("");
    public static DependencyRule MAVEN_PLUGINS_KEEP_VERSION_RULE = new DependencyRule("* * maven-plugin * * *");
    public static DependencyRule NO_CHANGE_RULE = new DependencyRule("* * * * * *");

    private Rule groupRule;
    private Rule artifactRule;
    private Rule typeRule;
    private Rule versionRule;
    private Rule scopeRule;

    public DependencyRule(String def) {
        StringTokenizer st = new StringTokenizer(def, " \t", false);
        if (st.hasMoreTokens()) {
            groupRule = new Rule(st.nextToken());
        } else {
            groupRule = new Rule("*");
        }
        if (st.hasMoreTokens()) {
            artifactRule = new Rule(st.nextToken());
        } else {
            artifactRule = new Rule("*");
        }
        if (st.hasMoreTokens()) {
            typeRule = new Rule(st.nextToken());
        } else {
            typeRule = new Rule("*");
        }
        if (st.hasMoreTokens()) {
            versionRule = new Rule(st.nextToken());
        } else {
            versionRule = new Rule("s/.*/debian/");
        }
        if (st.hasMoreTokens()) {
            scopeRule = new Rule(st.nextToken());
        } else {
            scopeRule = new Rule("*");
        }
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

    public boolean matches(Dependency dependency) {
        return groupRule.match(dependency.getGroupId()) && artifactRule.match(dependency.getArtifactId()) && typeRule.match(dependency.getType()) 
                && versionRule.match(dependency.getVersion()) && scopeRule.match(dependency.getScope());
    }

    public Dependency apply(Dependency dependency) {
        return new Dependency(groupRule.apply(dependency.getGroupId()), 
                artifactRule.apply(dependency.getArtifactId()),
                typeRule.apply(dependency.getType()),
                versionRule.apply(dependency.getVersion()),
                scopeRule.apply(dependency.getScope()),
                dependency.isOptional());
    }

    /**
     * Natural sort order: from the most specific rules to the most generic rules,
     * a rule is more generic if a generic match appears on th egroup rule, then
     * artifact rule, then type rule, then version rule.
     * If 2 rules have the same order of genericity, then use an alphabetical
     * sorting of the pattern strings.
     */
    public int compareTo(Object o) {
        DependencyRule other = (DependencyRule) o;
        if (groupRule.isGeneric() && !other.groupRule.isGeneric()) {
            return 1;
        }
        if (!groupRule.isGeneric() && other.groupRule.isGeneric()) {
            return -1;
        }
        if (artifactRule.isGeneric() && !other.artifactRule.isGeneric()) {
            return 1;
        }
        if (!artifactRule.isGeneric() && other.artifactRule.isGeneric()) {
            return -1;
        }
        if (typeRule.isGeneric() && !other.typeRule.isGeneric()) {
            return 1;
        }
        if (!typeRule.isGeneric() && other.typeRule.isGeneric()) {
            return -1;
        }
        if (versionRule.isGeneric() && !other.versionRule.isGeneric()) {
            return 1;
        }
        if (!versionRule.isGeneric() && other.versionRule.isGeneric()) {
            return -1;
        }
        if (scopeRule.isGeneric() && !other.scopeRule.isGeneric()) {
            return 1;
        }
        if (!scopeRule.isGeneric() && other.scopeRule.isGeneric()) {
            return -1;
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
        if (this.scopeRule != other.scopeRule && (this.scopeRule == null || !this.scopeRule.equals(other.scopeRule))) {
            return false;
        }
        return true;
    }

    public int hashCode() {
        int hash = 7;
        hash = 73 * hash + (this.groupRule != null ? this.groupRule.hashCode() : 0);
        hash = 73 * hash + (this.artifactRule != null ? this.artifactRule.hashCode() : 0);
        hash = 73 * hash + (this.typeRule != null ? this.typeRule.hashCode() : 0);
        hash = 73 * hash + (this.versionRule != null ? this.versionRule.hashCode() : 0);
        hash = 73 * hash + (this.scopeRule != null ? this.scopeRule.hashCode() : 0);
        return hash;
    }

    public String toPatternString() {
        return groupRule.getPattern() + ":" + artifactRule.getPattern() + ":" + typeRule.getPattern() + ":" + versionRule.getPattern()
                + ":" + scopeRule.getPattern();
    }

    public String toString() {
        return groupRule + " " + artifactRule + " " + typeRule + " " + versionRule + " " + scopeRule;
    }

}
