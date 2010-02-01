package org.debian.maven.repo;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

public class POMInfo {
    
    public static final String DEPENDENCY_MANAGEMENT_LIST = "DependencyManagement";
    public static final String PROFILE_DEPENDENCY_MANAGEMENT_LIST = "ProfileDependencyManagement";
    public static final String DEPENDENCIES = "Dependencies";
    public static final String PROFILE_DEPENDENCIES = "ProfileDependencies";
    public static final String PLUGIN_DEPENDENCIES = "PluginDependencies";
    public static final String PROFILE_PLUGIN_DEPENDENCIES = "ProfilePluginDependencies";
    public static final String PLUGINS = "Plugins";
    public static final String PLUGIN_MANAGEMENT = "PluginManagement";
    public static final String EXTENSIONS = "Extensions";
    public static final String MODULES = "Modules";

    private String originalParentVersion;
    private Dependency originalPom;
    private Dependency parent;
    private Dependency thisPom;
    private List modules;
    private List dependencies;
    private List dependencyManagement;
    private List extensions;
    private List plugins;
    private List pluginManagement;
    private List pluginDependencies;
    private List profileDependencies;
    private List profileDependencyManagement;
    private List profilePluginDependencies;
    private Map properties;
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

    public List getModules() {
        return modules;
    }

    public void setModules(List modules) {
        this.modules = modules;
    }

    /**
     * @return the dependencies
     */
    public List getDependencies() {
        return dependencies;
    }

    /**
     * @param dependencies the dependencies to set
     */
    public void setDependencies(List dependencies) {
        this.dependencies = dependencies;
    }

    /**
     * @return the properties
     */
    public Map getProperties() {
        return properties;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Map properties) {
        this.properties = properties;
    }

    public List getDependencyManagement() {
        return dependencyManagement;
    }

    public void setDependencyManagement(List dependencyManagement) {
        this.dependencyManagement = dependencyManagement;
    }

    public List getPlugins() {
        return plugins;
    }

    public void setPlugins(List plugins) {
        this.plugins = plugins;
    }

    public List getPluginManagement() {
        return pluginManagement;
    }

    public void setPluginManagement(List pluginManagement) {
        this.pluginManagement = pluginManagement;
    }

    public List getProfileDependencies() {
        return profileDependencies;
    }

    public void setProfileDependencies(List profileDependencies) {
        this.profileDependencies = profileDependencies;
    }

    public List getProfileDependencyManagement() {
        return profileDependencyManagement;
    }

    public void setProfileDependencyManagement(List profileDependencyManagement) {
        this.profileDependencyManagement = profileDependencyManagement;
    }

    public List getExtensions() {
        return extensions;
    }

    public void setExtensions(List extensions) {
        this.extensions = extensions;
    }

    public List getPluginDependencies() {
        return pluginDependencies;
    }

    public void setPluginDependencies(List pluginDependencies) {
        this.pluginDependencies = pluginDependencies;
    }

    public List getProfilePluginDependencies() {
        return profilePluginDependencies;
    }

    public void setProfilePluginDependencies(List profilePluginDependencies) {
        this.profilePluginDependencies = profilePluginDependencies;
    }

    public Set getPublishedRules(boolean includeDefault) {
        Set rules = new TreeSet();
        if (includeDefault) {
            rules.add(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
            rules.add(DependencyRule.TO_DEBIAN_VERSION_RULE);
        }
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

    public POMInfo applyRules(Collection rules) {
        if (rules.isEmpty()) {
            return this;
        }

        POMInfo result = new POMInfo();
        result.setOriginalPom(getThisPom());
        result.setThisPom(getThisPom().applyRules(rules));
        if (getParent() != null) {
            result.setOriginalParentVersion(getParent().getVersion());
            result.setParent(getParent().applyRules(rules));
        }
        result.setDependencies(Dependency.applyRules(getDependencies(), rules));
        result.setDependencyManagement(Dependency.applyRules(getDependencyManagement(), rules));
        result.setExtensions(Dependency.applyRules(getExtensions(), rules));
        result.setPlugins(Dependency.applyRules(getPlugins(), rules));
        result.setPluginDependencies(Dependency.applyRules(getPluginDependencies(), rules));
        result.setPluginManagement(Dependency.applyRules(getPluginManagement(), rules));
        result.setProfileDependencies(Dependency.applyRules(getProfileDependencies(), rules));
        result.setProfilePluginDependencies(Dependency.applyRules(getProfilePluginDependencies(), rules));
        result.setProfileDependencyManagement(Dependency.applyRules(getProfileDependencyManagement(), rules));
        result.setProperties(getProperties());
        result.setModules(getModules());

        return result;
    }

    public void applyRulesOnDependenciesAndPlugins(Collection rules) {
        setDependencies(Dependency.applyRules(getDependencies(), rules));
        setDependencyManagement(Dependency.applyRules(getDependencyManagement(), rules));
        setExtensions(Dependency.applyRules(getExtensions(), rules));
        setPlugins(Dependency.applyRules(getPlugins(), rules));
        setPluginDependencies(Dependency.applyRules(getPluginDependencies(), rules));
        setPluginManagement(Dependency.applyRules(getPluginManagement(), rules));
        setProfileDependencies(Dependency.applyRules(getProfileDependencies(), rules));
        setProfilePluginDependencies(Dependency.applyRules(getProfilePluginDependencies(), rules));
        setProfileDependencyManagement(Dependency.applyRules(getProfileDependencyManagement(), rules));
        setProperties(getProperties());
        setModules(getModules());
    }

    public void mergeManagement(POMInfo parentPOM) {
        this.parentPOM = parentPOM;
        if (parentPOM != null) {
            mergeManagement(dependencyManagement, parentPOM.getDependencyManagement());
            mergeManagement(pluginManagement, parentPOM.getPluginManagement());
        }
        resolveVersions(dependencies, dependencyManagement);
        resolveVersions(plugins, pluginManagement);
    }

    private void mergeManagement(List target, List management) {
        resolveVersions(target, management);
        nextParentDep: for (Iterator i = management.iterator(); i.hasNext();) {
            Dependency parentDep = (Dependency)i.next();
            for (Iterator j = target.iterator(); j.hasNext();) {
                Dependency dependency = (Dependency)j.next();
                if (dependency.equals(parentDep)) {
                    continue nextParentDep;
                }
            }
            target.add(parentDep);
        }
    }

    public String getVersionFromManagementDependency(Dependency dependency) {
        List allManagementLists = new ArrayList();
        allManagementLists.addAll(getDependencyList(DEPENDENCY_MANAGEMENT_LIST));
        allManagementLists.addAll(getDependencyList(PLUGIN_MANAGEMENT));
        for (Iterator i = allManagementLists.iterator(); i.hasNext();) {
            Dependency mgtDep = (Dependency)i.next();
            if (mgtDep.getGroupId().equals(dependency.getGroupId()) &&
                    mgtDep.getArtifactId().equals(dependency.getArtifactId()) ) {
                if (mgtDep.getVersion() != null) {
                    return mgtDep.getVersion();
                }
            }
        }

        if (parentPOM != null) {
            return parentPOM.getVersionFromManagementDependency(dependency);
        }
        return null;
    }

    private void resolveVersions(List deps, List management) {
        for (Iterator i = management.iterator(); i.hasNext();) {
            Dependency parentDep = (Dependency)i.next();
            for (Iterator j = deps.iterator(); j.hasNext();) {
                Dependency dependency = (Dependency)j.next();
                if (dependency.getVersion() == null) {
                    if (dependency.getGroupId().equals(parentDep.getGroupId()) &&
                        dependency.getArtifactId().equals(parentDep.getArtifactId()) &&
                        dependency.getType().equals(parentDep.getType())) {
                        dependency.setVersion(parentDep.getVersion());
                        break;
                    }
                }
            }
        }
    }

    public List getDependencyList(String listSelector) {
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
        if (PROFILE_PLUGIN_DEPENDENCIES.equals(listSelector)) {
            return getProfilePluginDependencies();
        }
        if (PLUGINS.equals(listSelector)) {
            return getPlugins();
        }
        if (PLUGIN_MANAGEMENT.equals(listSelector)) {
            return getPluginManagement();
        }
        if (EXTENSIONS.equals(listSelector)) {
            return getExtensions();
        }
        if (MODULES.equals(listSelector)) {
            return getModules();
        }
        return null;
    }
}
