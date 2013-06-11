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

import java.util.*;
import static org.debian.maven.repo.POMInfo.DependencyType.*;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class POMInfo implements Cloneable {

    private String originalParentVersion;
    private Dependency originalPom;
    private Dependency parent;
    private Dependency thisPom;
    private List<String> modules;
    private Map<DependencyType, List<Dependency>> dependencies = POMInfo.initDependenciesMultiMap();
    // Use a TreeMap to guarantee ordering of the keys.
    private TreeMap<String, String> properties;
    private POMInfo parentPOM;

    public Dependency getOriginalPom() {
        return originalPom;
    }

    public void setOriginalPom(Dependency originalPom) {
        this.originalPom = originalPom;
    }

    public String getOriginalVersion() {
        if (originalPom != null) {
            return originalPom.getVersion();
        }
        return thisPom.getVersion();
    }

    public String getOriginalParentVersion() {
        return originalParentVersion;
    }

    public void setOriginalParentVersion(String originalParentVersion) {
        this.originalParentVersion = originalParentVersion;
    }

    public Dependency getParent() {
        return parent;
    }

    public void setParent(Dependency parent) {
        this.parent = parent;
    }

    public Dependency getThisPom() {
        return thisPom;
    }

    public void setThisPom(Dependency thisPom) {
        this.thisPom = thisPom;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    public TreeMap<String, String> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, String> properties) {
        this.properties = new TreeMap<String, String>(properties);
    }

    public Set<DependencyRule> getPublishedRules() {
        Set<DependencyRule> rules = new TreeSet<DependencyRule>();
        if (getProperties() == null) {
            return rules;
        }
        String mavenRules = getProperties().get("debian.mavenRules");
        if (mavenRules != null) {
            for(String ruleString : mavenRules.split(",")) {
                rules.add(new DependencyRule(ruleString.trim()));
            }
        }
        return rules;
    }

    public Map<DependencyType, List<Dependency>> getDependencies() {
        return dependencies;
    }

    public void setDependencies(Map<DependencyType, List<Dependency>> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * Create a new POM from the current POM, and apply all rules on the dependencies and parent POM
     * @param rules The list of rules to apply
     * @see #applyRulesOnParent(java.util.Collection, Repository) 
     */
    public POMInfo newPOMFromRules(Collection<DependencyRule> rules, Repository repository) {
        if (rules.isEmpty()) {
            return this;
        }

        POMInfo result = new POMInfo();
        result.setOriginalPom(getThisPom());
        result.setThisPom(getThisPom().applyRules(rules));

        Map<DependencyType, List<Dependency>> dependencies = new EnumMap<DependencyType, List<Dependency>>(DependencyType.class);
        for(DependencyType depType : DependencyType.values()) {
            dependencies.put(depType, Dependency.applyRules(this.dependencies.get(depType), rules));
        }
        result.setDependencies(dependencies);

        result.setProperties(new TreeMap<String, String>(getProperties()));
        result.setModules(getModules());

        result.setParent(getParent());
        result.applyRulesOnParent(rules, repository);

        return result;
    }

    public void applyRulesOnParent(Collection<DependencyRule> rules, Repository repository) {
        if (getParent() != null) {
            //System.out.println(this.getThisPom() + ": Apply rules on parent " + getParent() + " using repository " + repository);
            setOriginalParentVersion(getParent().getVersion());
            setParent(getParent().applyRules(rules));
            if (repository != null) {
                POMInfo newParentPOM = repository.getPOM(getParent());
                setParentPOM(newParentPOM);
            } else {
                // Always apply rules
                //System.out.println(this.getThisPom() + ": Parent is not registered in repository, apply rules anyway");
            }
        }
    }

    public void applyRulesOnDependenciesAndPlugins(Collection<DependencyRule> rules) {
        for(DependencyType depType : DependencyType.values()) {
            dependencies.put(depType, Dependency.applyRules(dependencies.get(depType), rules));
        }
    }

    public void applyIgnoreRulesOnDependenciesAndPlugins(Set<DependencyRule> rules) {
        for(DependencyType depType : DependencyType.values()) {
            dependencies.put(depType, Dependency.applyIgnoreRules(dependencies.get(depType), rules));
        }
    }

    public void setParentPOM(POMInfo parentPOM) {
        this.parentPOM = parentPOM;
        if (parentPOM != null) {
            if (this.parent == null) {
                this.parent = parentPOM.getThisPom();
            }
            mergeManagement(dependencies.get(DEPENDENCY_MANAGEMENT_LIST),
                parentPOM.getDependencies().get(DEPENDENCY_MANAGEMENT_LIST));

            mergeManagement(dependencies.get(PLUGIN_MANAGEMENT),
                parentPOM.getDependencies().get(PLUGIN_MANAGEMENT));
        }
        resolveVersions(dependencies.get(DEPENDENCIES), dependencies.get(DEPENDENCY_MANAGEMENT_LIST));
        resolveVersions(dependencies.get(PLUGIN_DEPENDENCIES), dependencies.get(DEPENDENCY_MANAGEMENT_LIST));
        resolveVersions(dependencies.get(PLUGIN_DEPENDENCIES), dependencies.get(PLUGIN_MANAGEMENT_DEPENDENCIES));
        resolveVersions(dependencies.get(PLUGINS), dependencies.get(PLUGIN_MANAGEMENT));
        resolveVersions(dependencies.get(REPORTING_PLUGINS), dependencies.get(PLUGIN_MANAGEMENT));
        resolveVersions(dependencies.get(PROFILE_DEPENDENCIES), dependencies.get(PROFILE_DEPENDENCY_MANAGEMENT_LIST));
        resolveVersions(dependencies.get(PROFILE_PLUGINS), dependencies.get(PROFILE_PLUGIN_MANAGEMENT));
    }

    private void mergeManagement(List<Dependency> target, List<Dependency> management) {
        resolveVersions(target, management);
        nextParentDep: for (Dependency parentDep: management) {
            for (Dependency dependency: target) {
                if (dependency.equalsIgnoreVersion(parentDep)) {
                    continue nextParentDep;
                }
            }
            target.add(new Dependency(parentDep));
        }
    }

    public String getVersionFromManagementDependency(Dependency dependency) {
        for (DependencyType depType: Arrays.asList(DEPENDENCY_MANAGEMENT_LIST, PLUGIN_MANAGEMENT, PLUGIN_MANAGEMENT_DEPENDENCIES)) {
            for (Dependency mgtDep: dependencies.get(depType)) {
                if (mgtDep.equalsIgnoreVersion(dependency) ) {
                    if (mgtDep.getVersion() != null) {
                        //System.out.println("In " + getThisPom() + " - found version " + mgtDep.getVersion() + " for " +
                        //  dependency.getGroupId() + ":" + dependency.getArtifactId() + " according to the list of " + listType);
                        return mgtDep.getVersion();
                    }
                }
            }
        }

        if (parentPOM != null) {
            return parentPOM.getVersionFromManagementDependency(dependency);
        }
        return null;
    }

    private void resolveVersions(List<Dependency> deps, List<Dependency> management) {
        for (Dependency parentDep: management) {
            for (Dependency dependency: deps) {
                if (dependency.getVersion() == null || !dependency.isScopeDefined()) {
                    if (dependency.getGroupId().equals(parentDep.getGroupId()) &&
                        dependency.getArtifactId().equals(parentDep.getArtifactId()) &&
                        dependency.getType().equals(parentDep.getType())) {

                        if (dependency.getVersion() == null) {
                            //System.out.println("In " + getThisPom() + " - resolve version to " + parentDep.getVersion() + " for " +
                            //  dependency.getGroupId() + ":" + dependency.getArtifactId() + " according to parent POM");
                            dependency.setVersion(parentDep.getVersion());
                        }
                        if (!dependency.isScopeDefined()) {
                            dependency.setScope(parentDep.getScope());
                        }

                        break;
                    }
                }
            }
        }
    }

    public Object clone() throws CloneNotSupportedException {
        POMInfo clone = new POMInfo();
        clone.originalParentVersion = originalParentVersion;
        clone.originalPom = originalPom;
        clone.parent = parent;
        clone.thisPom = (Dependency) thisPom.clone();
        clone.modules = modules;
        // TODO this doesn't really make sense. We should do a deep copy here.
        // But lets see first, who uses the clone method!
        clone.dependencies = dependencies;
        clone.properties = properties;
        clone.parentPOM = parentPOM;
        return clone;
    }

    public static Map<DependencyType, List<Dependency>> initDependenciesMultiMap() {
        Map<DependencyType, List<Dependency>> dependencies = new EnumMap<DependencyType, List<Dependency>>(DependencyType.class);
        for (DependencyType depType : DependencyType.values()) {
            dependencies.put(depType, new ArrayList<Dependency>());
        }
        return dependencies;
    }

    public enum DependencyType {
        // The order of the patterns is important! More specific patterns first!
        DEPENDENCY_MANAGEMENT_LIST("/project/dependencyManagement/dependencies/dependency"),
        PROFILE_DEPENDENCY_MANAGEMENT_LIST("profile/dependencyManagement/dependencies/dependency"),
        DEPENDENCIES("/project/dependencies/dependency"),
        PROFILE_DEPENDENCIES("profile/dependencies/dependency"),
        PLUGIN_DEPENDENCIES("/project/*/*/plugin/dependencies/dependency"),
        PLUGIN_MANAGEMENT_DEPENDENCIES("build/*/*/plugin/dependencies/dependency"),
        PROFILE_PLUGIN_DEPENDENCIES("profile/*/*/plugin/dependencies/dependency"),

        PROFILE_PLUGIN_MANAGEMENT("profile/*/pluginManagement/plugins/plugin"),
        PLUGIN_MANAGEMENT("pluginManagement/plugins/plugin"),
        PROFILE_REPORTING_PLUGINS("profile/reporting/plugins/plugin"),
        REPORTING_PLUGINS("reporting/plugins/plugin"),
        PROFILE_PLUGINS("profile/*/*/plugin"),
        PLUGINS("plugin"),

        EXTENSIONS("extension");

        private static final Map<String, DependencyType> byPatternMap = new HashMap<String, DependencyType>();
        static {
            for(DependencyType type : values()) {
                byPatternMap.put(type.pattern, type);
            }
        }
        public final String pattern;

        DependencyType(String pattern) {
            this.pattern = pattern;
        }

        Dependency dependencyPrototype() {
            if (pattern.endsWith("plugin")) {
                return new Dependency(Dependency.PROTO_PLUGIN);
            } else {
                return new Dependency(Dependency.PROTO_JAR);
            }
        }

        public DependencyType byPattern(String pattern) {
            return byPatternMap.get(pattern);
        }
    }
}
