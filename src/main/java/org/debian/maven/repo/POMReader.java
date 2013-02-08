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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Reads relevant information from the POM.
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 * @author Damien Raude-Morvan <drazzib@debian.org>
 */
public class POMReader {

    private static final List<String> READ_IGNORED_ELEMENTS = Arrays.asList(
                "distributionManagement", "ciManagement", "prerequisites", "exclusions",
                "repositories", "pluginRepositories", "reports", "modelVersion");
    private static final List<String> PLUGIN_IGNORED_ELEMENTS = Arrays.asList(
                "executions", "configuration", "goals", "reportSets" );

    protected final XMLInputFactory factory = XMLInputFactory.newInstance();

    public POMInfo readPom(File originalPom) throws XMLStreamException, FileNotFoundException {
        if (!originalPom.exists()) {
            System.err.println("Cannot find pom file " + originalPom.getAbsolutePath());
        }
        return readPom(new FileReader(originalPom));
    }

    public POMInfo readPom(Reader originalPom) throws XMLStreamException {
        XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(originalPom));
        
        // Stack of the XML path currently parsed. Most deepest XML element is first in the list.
        TreePath<String> path = new TreePath<String>();
        
        List<Dependency> dependencies = new ArrayList<Dependency>();
        List<Dependency> dependencyManagement = new ArrayList<Dependency>();
        List<Dependency> extensions = new ArrayList<Dependency>();
        List<Dependency> plugins = new ArrayList<Dependency>();
        List<Dependency> pluginManagement = new ArrayList<Dependency>();
        List<Dependency> pluginManagementDependencies = new ArrayList<Dependency>();
        List<Dependency> pluginDependencies = new ArrayList<Dependency>();
        List<Dependency> reportingPlugins = new ArrayList<Dependency>();
        List<Dependency> profileDependencies = new ArrayList<Dependency>();
        List<Dependency> profileDependencyManagement = new ArrayList<Dependency>();
        List<Dependency> profilePlugins = new ArrayList<Dependency>();
        List<Dependency> profilePluginDependencies = new ArrayList<Dependency>();
        List<Dependency> profilePluginManagement = new ArrayList<Dependency>();
        List<Dependency> profileReportingPlugins = new ArrayList<Dependency>();

        // http://maven.apache.org/pom.html#Aggregation:
        // "the ordering of the modules [...] is not important"
        // However the POMTransformer depends on the ordering...
        List<String> modules = new ArrayList<String>();

        Map<String, String> properties = new TreeMap<String, String>();
        Dependency thisPom = new Dependency(null, null, "jar", null);
        Dependency parent = null;
        Dependency currentDependency = null;
        int inIgnoredElement = 0;
        String element = null;

        // First pass - collect version and parent information
        for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    element = parser.getLocalName();
                    path.add(element);
                    if (isReadIgnoredElement(element) ||
                            (path.contains("plugin") && PLUGIN_IGNORED_ELEMENTS.contains(element)) ||
                            (path.contains("dependency") && "exclusions".equals(element)) ||
                            inIgnoredElement > 0) {
                        inIgnoredElement++;
                    } else if (path.contains("exclusions")) {
                        // nothing to do
                    } else if ("dependency".equals(element)) {
                        currentDependency = new Dependency(null, null, "jar", null);
                        if(path.matches("project/dependencyManagement/dependencies/dependency"))
                            dependencyManagement.add(currentDependency);
                        else if(path.matches("profile/dependencyManagement/dependencies/dependency"))
                            profileDependencyManagement.add(currentDependency);
                        else if(path.matches("project/dependencies/dependency"))
                            dependencies.add(currentDependency);
                        else if(path.matches("profile/dependencies/dependency"))
                            profileDependencies.add(currentDependency);
                        else if(path.matches("project/*/*/plugin/dependencies/dependency"))
                            pluginDependencies.add(currentDependency);
                        else if(path.matches("build/*/*/plugin/dependencies/dependency"))
                            pluginManagementDependencies.add(currentDependency);
                        else if(path.matches("profile/*/*/plugin/dependencies/dependency"))
                            profilePluginDependencies.add(currentDependency);
                        else {
                            System.err.println("Unexpected element: " + path.parent(1));
                        }
                    } else if (path.contains("dependency")) {
                        // Nothing to do, path.parent(0) == "dependency" handled before!
                    } else if ("plugin".equals(element)) {
                        currentDependency = new Dependency("org.apache.maven.plugins", null, "maven-plugin", null);

                        if(path.matches("profile/*/pluginManagement/plugins/plugin"))
                            profilePluginManagement.add(currentDependency);
                        else if(path.matches("pluginManagement/plugins/plugin"))
                            pluginManagement.add(currentDependency);
                        else if(path.matches("profile/reporting/plugins/plugin"))
                            profileReportingPlugins.add(currentDependency);
                        else if(path.matches("reporting/plugins/plugin"))
                            reportingPlugins.add(currentDependency);
                        else if(path.matches("profile/*/*/plugin"))
                            profilePlugins.add(currentDependency);
                        else if(path.matches("plugin"))
                            plugins.add(currentDependency);

                    } else if (path.contains("plugin")) {
                        // nothing to do, path.parent(0) == "plugin" handled before!
                    } else if (path.matches("extension")) {
                        currentDependency = new Dependency(null, null, "jar", null);
                        extensions.add(currentDependency);
                    } else if (path.contains("extension") || path.contains("modules")) {
                        // nothing to do
                    } else if (path.size() == 2 && "parent".equals(element)) {
                        parent = new Dependency(null, null, "pom", null);
                    } else if (path.size() == 3 && "properties".equals(path.parent(1))) {
                        // in case the property does not contain any text, might be overwritten be the nested characters
                        properties.put(element, "true");
                    }
                    break;
                }

                case XMLStreamConstants.END_ELEMENT: {
                    path.remove();
                    if (inIgnoredElement > 0) {
                        inIgnoredElement--;
                    }
                    element = null;
                    break;
                }

                case XMLStreamConstants.CHARACTERS: {
                    String value = parser.getText().trim();
                    if (inIgnoredElement > 0 || path.contains("exclusions")) {
                        // ignore
                    } else if (path.contains("dependency") || path.contains("plugin") || path.contains("extension")) {
                        if ("groupId".equals(element)) {
                            currentDependency.setGroupId(value);
                        } else if ("artifactId".equals(element)) {
                            currentDependency.setArtifactId(value);
                        } else if ("type".equals(element)) {
                            currentDependency.setType(value);
                        } else if ("version".equals(element)) {
                            currentDependency.setVersion(value);
                        } else if ("optional".equals(element)) {
                            currentDependency.setOptional("true".equals(value));
                        } else if ("scope".equals(element)) {
                            currentDependency.setScope(value);
                        } else if ("classifier".equals(element)) {
                            currentDependency.setClassifier(value);
                        }
                    } else if (path.size() == 3 && "modules".equals(path.parent(1))) {
                        // we're not interested in the modules section inside a profiles section
                        modules.add(value);
                    } else if (path.size() == 3 && "parent".equals(path.parent(1))) {
                        if ("groupId".equals(element)) {
                            parent.setGroupId(value);
                        } else if ("artifactId".equals(element)) {
                            parent.setArtifactId(value);
                        } else if ("version".equals(element)) {
                            parent.setVersion(value);
                        } else if ("relativePath".equals(element)) {
                            parent.setRelativePath(value);
                        }
                    } else if (path.size() == 3 && "properties".equals(path.parent(1))) {
                        properties.put(element, value);
                    } else if (path.size() == 2 && inIgnoredElement == 0) {
                        if ("groupId".equals(element)) {
                            thisPom.setGroupId(value);
                        } else if ("artifactId".equals(element)) {
                            thisPom.setArtifactId(value);
                        } else if ("packaging".equals(element)) {
                            thisPom.setType(value);
                        } else if ("version".equals(element)) {
                            thisPom.setVersion(value);
                        }
                    }
                    break;
                }

                case XMLStreamConstants.CDATA: {
                    String value = parser.getText().trim();
                    if (path.size() == 3 && "properties".equals(path.parent(1))) {
                        properties.put(element, value);
                    }
                    break;
                }

            } // end switch
        } // end for
        parser.close();

        if (thisPom.getGroupId() == null && parent != null) {
            thisPom.setGroupId(parent.getGroupId());
        }
        if (thisPom.getVersion() == null && parent != null) {
            thisPom.setVersion(parent.getVersion());
        }

        Map<String, String> inferedProperties = new TreeMap<String, String>(properties);
        
        inferedProperties.put("pom.groupId", thisPom.getGroupId());
        inferedProperties.put("project.groupId", thisPom.getGroupId());
        inferedProperties.put("groupId", thisPom.getGroupId());
        
        inferedProperties.put("pom.artifactId", thisPom.getArtifactId());
        inferedProperties.put("project.artifactId", thisPom.getArtifactId());
        inferedProperties.put("artifactId", thisPom.getArtifactId());
        
        inferedProperties.put("pom.version", thisPom.getVersion());
        inferedProperties.put("project.version", thisPom.getVersion());
        inferedProperties.put("version", thisPom.getVersion());
        
        if (parent != null) {
            inferedProperties.put("parent.groupId", parent.getGroupId());
            inferedProperties.put("parent.artifactId", parent.getArtifactId());
            inferedProperties.put("parent.version", parent.getVersion());
            inferedProperties.put("project.parent.groupId", parent.getGroupId());
            inferedProperties.put("project.parent.artifactId", parent.getArtifactId());
            inferedProperties.put("project.parent.version", parent.getVersion());
        }
        
        expandProperties(thisPom, inferedProperties);
        expendProperties(dependencies, inferedProperties);
        expendProperties(dependencyManagement, inferedProperties);
        expendProperties(plugins, inferedProperties);
        expendProperties(pluginManagement, inferedProperties);
        expendProperties(pluginDependencies, inferedProperties);
        expendProperties(pluginManagementDependencies, inferedProperties);
        expendProperties(reportingPlugins, inferedProperties);
        expendProperties(profileDependencies, inferedProperties);
        expendProperties(profileDependencyManagement, inferedProperties);
        expendProperties(profilePlugins, inferedProperties);
        expendProperties(profilePluginDependencies, inferedProperties);
        expendProperties(profilePluginManagement, inferedProperties);
        expendProperties(profileReportingPlugins, inferedProperties);

        POMInfo info = new POMInfo();
        if (properties.get("debian.originalVersion") != null) {
            Dependency originalPomDep = new Dependency(thisPom.getGroupId(),
                    thisPom.getArtifactId(), thisPom.getType(),
                    properties.get("debian.originalVersion"));
            info.setOriginalPom(originalPomDep);
        }
        info.setThisPom(thisPom);
        info.setParent(parent);
        info.setModules(new ArrayList<String>(modules));
        info.setDependencies(dependencies);
        info.setDependencyManagement(dependencyManagement);
        info.setExtensions(extensions);
        info.setPlugins(plugins);
        info.setPluginManagement(pluginManagement);
        info.setPluginDependencies(pluginDependencies);
        info.setPluginManagementDependencies(pluginManagementDependencies);
        info.setReportingPlugins(reportingPlugins);
        info.setProfileDependencies(profileDependencies);
        info.setProfileDependencyManagement(profileDependencyManagement);
        info.setProfilePlugins(profilePlugins);
        info.setProfilePluginDependencies(profilePluginDependencies);
        info.setProfilePluginManagement(profilePluginManagement);
        info.setProfileReportingPlugins(profileReportingPlugins);
        info.setProperties(properties);
        return info;
    }

    protected boolean isReadIgnoredElement(String element) {
        return READ_IGNORED_ELEMENTS.contains(element);
    }

    private void expendProperties(List<Dependency> dependencies, Map<String, String> inferedProperties) {
        for (Dependency dependency : dependencies) {
            expandProperties(dependency, inferedProperties);
        }
    }

    private void expandProperties(Dependency dependency, Map<String, String> inferedProperties) {
        dependency.setGroupId(expandString(dependency.getGroupId(), inferedProperties));
        dependency.setArtifactId(expandString(dependency.getArtifactId(), inferedProperties));
        dependency.setType(expandString(dependency.getType(), inferedProperties));
        dependency.setVersion(expandString(dependency.getVersion(), inferedProperties));
    }

    private String expandString(String str, Map<String, String> inferedProperties) {
        if (str == null) {
            return null;
        }
        int pos;
        while ((pos = str.indexOf("${")) >= 0) {
            int end = str.indexOf('}', pos);
            String property = str.substring(pos + 2, end);
            if (inferedProperties.containsKey(property)) {
                String endStr = "";
                if (end + 1 < str.length()) {
                    endStr = str.substring(end + 1);
                }
                str = str.substring(0, pos) + inferedProperties.get(property) + endStr;
            } else {
                break;
            }
        }
        return str;
    }

    static class TreePath<S> {
        private LinkedList<S> path = new LinkedList<S>();

        // forwarding functions to the inner LinkedList
        public void add(S el) { path.addLast(el); }
        public void remove() { path.removeLast(); }
        public boolean contains(S el) { return path.contains(el); }
        public int size() { return path.size(); }

        public S parent(int generations) {
            int index = (path.size() - 1) - generations;
            return index >= 0 ? path.get(index) : null;
        }

        public boolean matches(String patternString) {
            String[] pattern = patternString.split("/");
            int pathIndex = path.size() - pattern.length - 1;
            if(pathIndex < -1) return false;

            for(int i=0; i<pattern.length; ++i) {
                ++pathIndex;
                String patternElement = pattern[i];
                if("*".equals(patternElement)) continue;
                if(!patternElement.equals(path.get(pathIndex)) ) return false;
            }
            return true;
        }
    }
}
