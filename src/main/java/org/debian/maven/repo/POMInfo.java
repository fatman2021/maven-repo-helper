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
public class POMInfo implements Cloneable {
    
    public static final String DEPENDENCY_MANAGEMENT_LIST = "DependencyManagement";
    public static final String PROFILE_DEPENDENCY_MANAGEMENT_LIST = "ProfileDependencyManagement";
    public static final String DEPENDENCIES = "Dependencies";
    public static final String PROFILE_DEPENDENCIES = "ProfileDependencies";
    public static final String PLUGIN_DEPENDENCIES = "PluginDependencies";
    public static final String REPORTING_PLUGINS = "ReportingDependencies";
    public static final String PROFILE_PLUGINS = "ProfilePlugins";
    public static final String PROFILE_PLUGIN_DEPENDENCIES = "ProfilePluginDependencies";
    public static final String PROFILE_PLUGIN_MANAGEMENT = "ProfilePluginManagement";
    public static final String PROFILE_REPORTING_PLUGINS = "ProfileReportingPlugins";
    public static final String PLUGINS = "Plugins";
    public static final String PLUGIN_MANAGEMENT = "PluginManagement";
    public static final String PLUGIN_MANAGEMENT_DEPENDENCIES = "PluginManagementDependencies";
    public static final String EXTENSIONS = "Extensions";
    public static final String MODULES = "Modules";
    public static final String PARENT = "Parent";

    private String originalParentVersion;
    private Dependency originalPom;
    private Dependency parent;
    private Dependency thisPom;
    private List<String> modules;
    private List<Dependency> dependencies;
    private List<Dependency> dependencyManagement;
    private List<Dependency> extensions;
    private List<Dependency> plugins;
    private List<Dependency> pluginManagement;
    private List<Dependency> pluginDependencies;
    private List<Dependency> pluginManagementDependencies;
    private List<Dependency> reportingPlugins;
    private List<Dependency> profileDependencies;
    private List<Dependency> profileDependencyManagement;
    private List<Dependency> profilePlugins;
    private List<Dependency> profilePluginDependencies;
    private List<Dependency> profilePluginManagement;
    private List<Dependency> profileReportingPlugins;
    private Map<String, String> properties;
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

    /**
     * @return the parent
     */
    public Dependency getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(Dependency parent) {
        this.parent = parent;
    }

    /**
     * @return the thisPom
     */
    public Dependency getThisPom() {
        return thisPom;
    }

    /**
     * @param thisPom the thisPom to set
     */
    public void setThisPom(Dependency thisPom) {
        this.thisPom = thisPom;
    }

    public List<String> getModules() {
        return modules;
    }

    public void setModules(List<String> modules) {
        this.modules = modules;
    }

    /**
     * @return the dependencies
     */
    public List<Dependency> getDependencies() {
        return dependencies;
    }

    /**
     * @param dependencies the dependencies to set
     */
    public void setDependencies(List<Dependency> dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @return the properties
     */
    public Map<String, String> getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map<String, String> properties) {
        this.properties = properties;
    }

    public List<Dependency> getDependencyManagement() {
        return dependencyManagement;
    }

    public void setDependencyManagement(List<Dependency> dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    public List<Dependency> getPlugins() {
        return plugins;
    }

    public void setPlugins(List<Dependency> plugins) {
        this.plugins = plugins;
    }

    public List<Dependency> getPluginManagement() {
        return pluginManagement;
    }

    public void setPluginManagement(List<Dependency> pluginManagement) {
        this.pluginManagement = pluginManagement;
    }

    public List<Dependency> getPluginManagementDependencies() {
        return pluginManagementDependencies;
    }

    public void setPluginManagementDependencies(List<Dependency> pluginManagementDependencies) {
        this.pluginManagementDependencies = pluginManagementDependencies;
    }

    public List<Dependency> getProfileDependencies() {
        return profileDependencies;
    }

    public void setProfileDependencies(List<Dependency> profileDependencies) {
        this.profileDependencies = profileDependencies;
    }

    public List<Dependency> getProfileDependencyManagement() {
        return profileDependencyManagement;
    }

    public void setProfileDependencyManagement(List<Dependency> profileDependencyManagement) {
        this.profileDependencyManagement = profileDependencyManagement;
    }

    public List<Dependency> getExtensions() {
        return extensions;
    }

    public void setExtensions(List<Dependency> extensions) {
        this.extensions = extensions;
    }

    public List<Dependency> getPluginDependencies() {
        return pluginDependencies;
    }

    public void setPluginDependencies(List<Dependency> pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
    }

    public List<Dependency> getReportingPlugins() {
        return reportingPlugins;
    }

    public void setReportingPlugins(List<Dependency> reportingPlugins) {
        this.reportingPlugins = reportingPlugins;
    }

    public List<Dependency> getProfilePlugins() {
        return profilePlugins;
    }

    public void setProfilePlugins(List<Dependency> profilePlugins) {
        this.profilePlugins = profilePlugins;
    }

    public List<Dependency> getProfilePluginDependencies() {
        return profilePluginDependencies;
    }

    public void setProfilePluginDependencies(List<Dependency> profilePluginDependencies) {
        this.profilePluginDependencies = profilePluginDependencies;
    }

    public List<Dependency> getProfilePluginManagement() {
        return profilePluginManagement;
    }

    public void setProfilePluginManagement(List<Dependency> profilePluginManagement) {
        this.profilePluginManagement = profilePluginManagement;
    }

    public List<Dependency> getProfileReportingPlugins() {
        return profileReportingPlugins;
    }

    public void setProfileReportingPlugins(List<Dependency> profileReportingPlugins) {
        this.profileReportingPlugins = profileReportingPlugins;
    }

    public List<Dependency> getParentAsList() {
        List<Dependency> parentList = new ArrayList<Dependency>();
        if (getParent() != null) {
            parentList.add(getParent());
        }
        return parentList;
    }

    public Set<DependencyRule> getPublishedRules() {
        Set<DependencyRule> rules = new TreeSet<DependencyRule>();
        if (getProperties() == null) {
            return rules;
        }
        String mavenRules = (String) getProperties().get("debian.mavenRules");
        if (mavenRules != null) {
            StringTokenizer st = new StringTokenizer(mavenRules, ",");
            while (st.hasMoreTokens()) {
                String rule = st.nextToken().trim();
                rules.add(new DependencyRule(rule));
            }
        }
        return rules;
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
        result.setDependencies(Dependency.applyRules(getDependencies(), rules));
        result.setDependencyManagement(Dependency.applyRules(getDependencyManagement(), rules));
        result.setExtensions(Dependency.applyRules(getExtensions(), rules));
        result.setPlugins(Dependency.applyRules(getPlugins(), rules));
        result.setPluginDependencies(Dependency.applyRules(getPluginDependencies(), rules));
        result.setPluginManagement(Dependency.applyRules(getPluginManagement(), rules));
        result.setPluginManagementDependencies(Dependency.applyRules(getPluginManagementDependencies(), rules));
        result.setReportingPlugins(Dependency.applyRules(getReportingPlugins(), rules));
        result.setProfileDependencies(Dependency.applyRules(getProfileDependencies(), rules));
        result.setProfilePlugins(Dependency.applyRules(getProfilePlugins(), rules));
        result.setProfilePluginDependencies(Dependency.applyRules(getProfilePluginDependencies(), rules));
        result.setProfilePluginManagement(Dependency.applyRules(getProfilePluginManagement(), rules));
        result.setProfileDependencyManagement(Dependency.applyRules(getProfileDependencyManagement(), rules));
        result.setProfileReportingPlugins(Dependency.applyRules(getProfileReportingPlugins(), rules));

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
        setDependencies(Dependency.applyRules(getDependencies(), rules));
        setDependencyManagement(Dependency.applyRules(getDependencyManagement(), rules));
        setExtensions(Dependency.applyRules(getExtensions(), rules));
        setPlugins(Dependency.applyRules(getPlugins(), rules));
        setPluginDependencies(Dependency.applyRules(getPluginDependencies(), rules));
        setPluginManagement(Dependency.applyRules(getPluginManagement(), rules));
        setPluginManagementDependencies(Dependency.applyRules(getPluginManagementDependencies(), rules));
        setReportingPlugins(Dependency.applyRules(getReportingPlugins(), rules));
        setProfileDependencies(Dependency.applyRules(getProfileDependencies(), rules));
        setProfilePlugins(Dependency.applyRules(getProfilePlugins(), rules));
        setProfilePluginDependencies(Dependency.applyRules(getProfilePluginDependencies(), rules));
        setProfilePluginManagement(Dependency.applyRules(getProfilePluginManagement(), rules));
        setProfileDependencyManagement(Dependency.applyRules(getProfileDependencyManagement(), rules));
        setProfileReportingPlugins(Dependency.applyRules(getProfileReportingPlugins(), rules));
    }

    public void applyIgnoreRulesOnDependenciesAndPlugins(Set<DependencyRule> rules) {
        setDependencies(Dependency.applyIgnoreRules(getDependencies(), rules));
        setDependencyManagement(Dependency.applyIgnoreRules(getDependencyManagement(), rules));
        setExtensions(Dependency.applyIgnoreRules(getExtensions(), rules));
        setPlugins(Dependency.applyIgnoreRules(getPlugins(), rules));
        setPluginDependencies(Dependency.applyIgnoreRules(getPluginDependencies(), rules));
        setPluginManagement(Dependency.applyIgnoreRules(getPluginManagement(), rules));
        setPluginManagementDependencies(Dependency.applyIgnoreRules(getPluginManagementDependencies(), rules));
        setReportingPlugins(Dependency.applyIgnoreRules(getReportingPlugins(), rules));
        setProfileDependencies(Dependency.applyIgnoreRules(getProfileDependencies(), rules));
        setProfilePlugins(Dependency.applyIgnoreRules(getProfilePlugins(), rules));
        setProfilePluginDependencies(Dependency.applyIgnoreRules(getProfilePluginDependencies(), rules));
        setProfilePluginManagement(Dependency.applyIgnoreRules(getProfilePluginManagement(), rules));
        setProfileDependencyManagement(Dependency.applyIgnoreRules(getProfileDependencyManagement(), rules));
        setProfileReportingPlugins(Dependency.applyIgnoreRules(getProfileReportingPlugins(), rules));
    }

    public void setParentPOM(POMInfo parentPOM) {
        this.parentPOM = parentPOM;
        if (parentPOM != null) {
            if (this.parent == null) {
                this.parent = parentPOM.getThisPom();
            }
            mergeManagement(dependencyManagement, parentPOM.getDependencyManagement());
            mergeManagement(pluginManagement, parentPOM.getPluginManagement());
        }
        resolveVersions(dependencies, dependencyManagement);
        resolveVersions(pluginDependencies, dependencyManagement);
        resolveVersions(pluginDependencies, pluginManagementDependencies);
        resolveVersions(plugins, pluginManagement);
        resolveVersions(reportingPlugins, pluginManagement);
        resolveVersions(profileDependencies, profileDependencyManagement);
        resolveVersions(profilePlugins, profilePluginManagement);
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
        for (String listType: Arrays.asList(DEPENDENCY_MANAGEMENT_LIST, PLUGIN_MANAGEMENT, PLUGIN_MANAGEMENT_DEPENDENCIES)) {
            for (Dependency mgtDep: getDependencyList(listType)) {
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

    public List<Dependency> getDependencyList(String listSelector) {
        if (DEPENDENCY_MANAGEMENT_LIST.equals(listSelector)) {
            return getDependencyManagement();
        }
        if (PROFILE_DEPENDENCY_MANAGEMENT_LIST.equals(listSelector)) {
            return getProfileDependencyManagement();
        }
        if (DEPENDENCIES.equals(listSelector)) {
            return getDependencies();
        }
        if (PROFILE_DEPENDENCIES.equals(listSelector)) {
            return getProfileDependencies();
        }
        if (PLUGIN_DEPENDENCIES.equals(listSelector)) {
            return getPluginDependencies();
        }
        if (PROFILE_PLUGINS.equals(listSelector)) {
            return getProfilePlugins();
        }
        if (PROFILE_PLUGIN_DEPENDENCIES.equals(listSelector)) {
            return getProfilePluginDependencies();
        }
        if (PROFILE_PLUGIN_MANAGEMENT.equals(listSelector)) {
            return getProfilePluginManagement();
        }
        if (PROFILE_REPORTING_PLUGINS.equals(listSelector)) {
            return getProfileReportingPlugins();
        }
        if (PLUGINS.equals(listSelector)) {
            return getPlugins();
        }
        if (PLUGIN_MANAGEMENT.equals(listSelector)) {
            return getPluginManagement();
        }
        if (PLUGIN_MANAGEMENT_DEPENDENCIES.equals(listSelector)) {
            return getPluginManagementDependencies();
        }
        if (REPORTING_PLUGINS.equals(listSelector)) {
            return getReportingPlugins();
        }
        if (EXTENSIONS.equals(listSelector)) {
            return getExtensions();
        }
        if (PARENT.equals(listSelector)) {
            return getParentAsList();
        }
        return null;
    }

    public List<Dependency> getAllDependencies(String listType) {
        List<Dependency> allDependencies = new ArrayList<Dependency>(getDependencyList(listType));
        if (getParent() != null) {
            List<Dependency> parentDependencies = parentPOM.getAllDependencies(listType);
            allDependencies.addAll(parentDependencies);
        }
        return allDependencies;
    }

    public Object clone() throws CloneNotSupportedException {
        POMInfo clone = new POMInfo();
        clone.originalParentVersion = originalParentVersion;
        clone.originalPom = originalPom;
        clone.parent = parent;
        clone.thisPom = (Dependency) thisPom.clone();
        clone.modules = modules;
        clone.dependencies = dependencies;
        clone.dependencyManagement = dependencyManagement;
        clone.extensions = extensions;
        clone.plugins = plugins;
        clone.pluginManagement = pluginManagement;
        clone.pluginDependencies = pluginDependencies;
        clone.pluginManagementDependencies = pluginManagementDependencies;
        clone.reportingPlugins = reportingPlugins;
        clone.profileDependencies = profileDependencies;
        clone.profileDependencyManagement = profileDependencyManagement;
        clone.profilePlugins = profilePlugins;
        clone.profilePluginDependencies = profilePluginDependencies;
        clone.profilePluginManagement = profilePluginManagement;
        clone.profileReportingPlugins = profileReportingPlugins;
        clone.properties = properties;
        clone.parentPOM = parentPOM;
        return clone;
    }
}
