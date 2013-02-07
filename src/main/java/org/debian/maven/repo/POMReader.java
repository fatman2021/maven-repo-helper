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
import java.util.List;
import java.util.Map;
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
        List<String> path = new ArrayList<String>();
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
        List<String> modules = new ArrayList<String>();

        Map<String, String> properties = new TreeMap<String, String>();
        Dependency thisPom = new Dependency(null, null, "jar", null);
        Dependency parent = null;
        Dependency currentDependency = null;
        int inLevel = 0;
        int inIgnoredElement = 0;
        int inModule = 0;
        int inDependency = 0;
        int inExtension = 0;
        int inPlugin = 0;
        int inExclusion = 0;
        int inParent = 0;
        int inProperties = 0;
        String element = null;

        // First pass - collect version and parent information
        for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    element = parser.getLocalName();
                    if (isReadIgnoredElement(element) ||
                            (inPlugin > 0 && PLUGIN_IGNORED_ELEMENTS.contains(element)) ||
                            (inDependency > 0 && "exclusions".equals(element)) ||
                            inIgnoredElement > 0) {
                        inIgnoredElement++;
                    } else {
                        inLevel++;
                        path.add(element);
                        if ("exclusions".equals(element) || inExclusion > 0) {
                            inExclusion++;
                        } else if ("dependency".equals(element)) {
                            inDependency++;
                            currentDependency = new Dependency(null, null, "jar", null);
                            String parentElement = path.get(path.size() - 2);
                            String parentParentElement = path.get(path.size() - 3);
                            if ("dependencies".equals(parentElement)) {
                                if ("dependencyManagement".equals(parentParentElement)) {
                                    String p3Element = path.get(path.size() - 4);
                                    if ("project".equals(p3Element)) {
                                        dependencyManagement.add(currentDependency);
                                    } else if ("profile".equals(p3Element)) {
                                        profileDependencyManagement.add(currentDependency);
                                    }
                                } else if ("project".equals(parentParentElement)) {
                                    dependencies.add(currentDependency);
                                } else if ("profile".equals(parentParentElement)) {
                                    profileDependencies.add(currentDependency);
                                } else if ("plugin".equals(parentParentElement)) {
                                    String p5Element = path.get(path.size() - 6);
                                    if ("project".equals(p5Element)) {
                                        pluginDependencies.add(currentDependency);
                                    } else if ("build".equals(p5Element)) {
                                        pluginManagementDependencies.add(currentDependency);
                                    } else if ("profile".equals(p5Element)) {
                                        profilePluginDependencies.add(currentDependency);
                                    }
                                }
                            } else {
                                System.err.println("Unexpected element: " + parentElement);
                            }
                        } else if (inDependency > 0) {
                            inDependency++;
                        } else if ("plugin".equals(element)) {
                            inPlugin++;
                            String parentElement = path.get(path.size() - 2);
                            String parentParentElement = path.get(path.size() - 3);
                            String parentParentParentElement = null;
                            String p4Element = null;
                            if (path.size() > 4) {
                                parentParentParentElement = path.get(path.size() - 4);
                            }
                            if (path.size() > 5) {
                                p4Element = path.get(path.size() - 5);
                            }
                            currentDependency = new Dependency("org.apache.maven.plugins", null, "maven-plugin", null);
                            if ("plugins".equals(parentElement)) {
                                if ("pluginManagement".equals(parentParentElement)) {
                                    if ("profile".equals(p4Element)) {
                                        profilePluginManagement.add(currentDependency);
                                    } else {
                                        pluginManagement.add(currentDependency);
                                    }
                                } else if ("reporting".equals(parentParentElement)) {
                                    if ("profile".equals(parentParentParentElement)) {
                                        profileReportingPlugins.add(currentDependency);
                                    } else {
                                        reportingPlugins.add(currentDependency);
                                    }
                                } else if ("profile".equals(parentParentParentElement)) {
                                    profilePlugins.add(currentDependency);
                                } else {
                                    plugins.add(currentDependency);
                                }
                            }
                        } else if (inPlugin > 0) {
                            inPlugin++;
                        } else if ("extension".equals(element)) {
                            inExtension++;
                            currentDependency = new Dependency(null, null, "jar", null);
                            extensions.add(currentDependency);
                        } else if (inExtension > 0) {
                            inExtension++;
                        } else if (inLevel == 2 && "modules".equals(element)) {
                            inModule++;
                        } else if (inModule > 0) {
                            inModule++;
                        } else if (inLevel == 2 && "parent".equals(element)) {
                            inParent++;
                            parent = new Dependency(null, null, "pom", null);
                        } else if (inParent > 0) {
                            inParent++;
                        } else if (inLevel == 2 && "properties".equals(element)) {
                            inProperties++;
                        } else if (inProperties == 1) {
                            properties.put(element, "true");
                            inProperties++;
                        } else if (inProperties > 0) {
                            inProperties++;
                        }
                    }
                    break;
                }

                case XMLStreamConstants.END_ELEMENT: {
                    if (inIgnoredElement > 0) {
                        inIgnoredElement--;
                    } else {
                        inLevel--;
                        path.remove(path.size() - 1);
                        if (inExclusion > 0) {
                            inExclusion--;
                        } else if (inDependency > 0) {
                            inDependency--;
                        } else if (inPlugin > 0) {
                            inPlugin--;
                        } else if (inExtension > 0) {
                            inExtension--;
                        } else if (inModule > 0) {
                            inModule--;
                        } else if (inParent > 0) {
                            inParent--;
                        } else if (inProperties > 0) {
                            inProperties--;
                        }
                    }
                    element = null;
                    break;
                }

                case XMLStreamConstants.CHARACTERS: {
                    String value = parser.getText().trim();
                    if (inIgnoredElement > 0 || inExclusion > 0) {
                        // ignore
                    } else if (inDependency > 1 || inPlugin > 1 || inExtension > 1) {
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
                    } else if (inModule > 1) {
                        modules.add(value);
                    } else if (inParent > 1) {
                        if ("groupId".equals(element)) {
                            parent.setGroupId(value);
                        } else if ("artifactId".equals(element)) {
                            parent.setArtifactId(value);
                        } else if ("version".equals(element)) {
                            parent.setVersion(value);
                        } else if ("relativePath".equals(element)) {
                            parent.setRelativePath(value);
                        }
                    } else if (inProperties > 1) {
                        properties.put(element, value);
                    } else if (inLevel == 2 && inIgnoredElement == 0) {
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
                    if (inProperties > 1) {
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
        info.setModules(modules);
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
}
