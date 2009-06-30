package org.debian.maven.repo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
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
 */
public class POMReader {

    private static final List READ_IGNORED_ELEMENTS = Arrays.asList(new String[]{
                "distributionManagement", "ciManagement", "prerequisites", "exclusions",
                "repositories", "pluginRepositories", "reports", "modelVersion"});
    protected final XMLInputFactory factory = XMLInputFactory.newInstance();

    public POMInfo readPom(File originalPom) throws XMLStreamException, FileNotFoundException {
        if (!originalPom.exists()) {
            System.err.println("Cannot find pom file " + originalPom.getAbsolutePath());
        }
        return readPom(new FileReader(originalPom));
    }

    public POMInfo readPom(Reader originalPom) throws XMLStreamException {
        XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(originalPom));
        List path = new ArrayList();
        List dependencies = new ArrayList();
        List dependencyManagement = new ArrayList();
        List extensions = new ArrayList();
        List plugins = new ArrayList();
        List pluginManagement = new ArrayList();
        List pluginDependencies = new ArrayList();
        List profileDependencies = new ArrayList();
        List profileDependencyManagement = new ArrayList();
        List profilePluginDependencies = new ArrayList();
        List modules = new ArrayList();

        Map properties = new TreeMap();
        Dependency thisPom = new Dependency(null, null, "jar", null);
        Dependency parent = null;
        Dependency currentDependency = null;
        int inLevel = 0;
        int inIgnoredElement = 0;
        int inModule = 0;
        int inDependency = 0;
        int inExtension = 0;
        int inPlugin = 0;
        int inParent = 0;
        int inProperties = 0;
        String element = null;

        // First pass - collect version and parent information
        for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    element = parser.getLocalName();
                    if (isReadIgnoredElement(element) || inIgnoredElement > 0) {
                        inIgnoredElement++;
                    } else {
                        inLevel++;
                        path.add(element);
                        if ("dependency".equals(element)) {
                            inDependency++;
                            currentDependency = new Dependency(null, null, "jar", null);
                            String parentElement = (String) path.get(path.size() - 2);
                            String parentParentElement = (String) path.get(path.size() - 3);
                            if ("dependencies".equals(parentElement)) {
                                if ("dependencyManagement".equals(parentParentElement)) {
                                    String p3Element = (String) path.get(path.size() - 4);
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
                                    String p5Element = (String) path.get(path.size() - 6);
                                    if ("project".equals(p5Element)) {
                                        pluginDependencies.add(currentDependency);
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
                            String parentElement = (String) path.get(path.size() - 2);
                            String parentParentElement = (String) path.get(path.size() - 3);
                            currentDependency = new Dependency("org.apache.maven.plugins", null, "maven-plugin", null);
                            if ("plugins".equals(parentElement)) {
                                if ("pluginManagement".equals(parentParentElement)) {
                                    pluginManagement.add(currentDependency);
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
                            parent = new Dependency();
                            parent.setType("pom");
                        } else if (inParent > 0) {
                            inParent++;
                        } else if (inLevel == 2 && "properties".equals(element)) {
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
                        if (inDependency > 0) {
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
                    if (inDependency > 1 || inPlugin > 1 || inExtension > 1) {
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

        Map inferedProperties = new TreeMap(properties);
        inferedProperties.put("pom.groupId", thisPom.getGroupId());
        inferedProperties.put("project.groupId", thisPom.getGroupId());
        inferedProperties.put("pom.artifactId", thisPom.getArtifactId());
        inferedProperties.put("project.artifactId", thisPom.getArtifactId());
        inferedProperties.put("pom.version", thisPom.getVersion());
        inferedProperties.put("project.version", thisPom.getVersion());
        expendProperties(dependencies, inferedProperties);
        expendProperties(dependencyManagement, inferedProperties);
        expendProperties(plugins, inferedProperties);
        expendProperties(pluginManagement, inferedProperties);
        expendProperties(pluginDependencies, inferedProperties);
        expendProperties(profileDependencies, inferedProperties);
        expendProperties(profileDependencyManagement, inferedProperties);
        expendProperties(profilePluginDependencies, inferedProperties);

        POMInfo info = new POMInfo();
        if (properties.get("debian.originalVersion") != null) {
            Dependency originalPomDep = new Dependency(thisPom.getGroupId(),
                    thisPom.getArtifactId(), thisPom.getType(),
                    (String) properties.get("debian.originalVersion"));
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
        info.setProfileDependencies(profileDependencies);
        info.setProfileDependencyManagement(profileDependencyManagement);
        info.setProfilePluginDependencies(profilePluginDependencies);
        info.setProperties(properties);
        return info;
    }

    protected boolean isReadIgnoredElement(String element) {
        return READ_IGNORED_ELEMENTS.contains(element);
    }

    private void expendProperties(List dependencies, Map inferedProperties) {
        for (Iterator i = dependencies.iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            expandProperties(dependency, inferedProperties);
        }
    }

    private void expandProperties(Dependency dependency, Map inferedProperties) {
        dependency.setGroupId(expandString(dependency.getGroupId(), inferedProperties));
        dependency.setArtifactId(expandString(dependency.getArtifactId(), inferedProperties));
        dependency.setType(expandString(dependency.getType(), inferedProperties));
        dependency.setVersion(expandString(dependency.getVersion(), inferedProperties));
    }

    private String expandString(String str, Map inferedProperties) {
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
