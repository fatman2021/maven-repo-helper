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
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class POMTransformer extends POMReader {

    private static final Logger log = Logger.getLogger(POMTransformer.class.getName());
    private static final List<String> WRITE_IGNORED_ELEMENTS = Arrays.asList("modelVersion", "parent");
    private static final List<String> DEBIAN_BUILD_IGNORED_ELEMENTS = Arrays.asList("distributionManagement", "repositories", "pluginRepositories");
    private static final List<String> DEBIAN_DOC_IGNORED_ELEMENTS = Arrays.asList("reports", "reporting", "site");
    private static final List<String> INFO_ELEMENTS = Arrays.asList("groupId",
            "artifactId", "packaging", "version");
    private static final Pattern compactDependencyNotationMatcher =
            Pattern.compile("(\\w[a-zA-Z0-9\\-_\\.]*):(\\w[a-zA-Z0-9\\-_]*):(\\d[a-zA-Z0-9\\-_\\.]*)");
    private DependencyRuleSet rules = new DependencyRuleSet("Rules", new File("debian/maven.rules"));
    private DependencyRuleSet automaticRules = new DependencyRuleSet("Automatic rules");
    private DependencyRuleSet publishedRules = new DependencyRuleSet("Published rules", new File("debian/maven.publishedRules"));
    private DependencyRuleSet ignoreRules = new DependencyRuleSet("Ignore rules", new File("debian/maven.ignoreRules"));
    private Map<File, Set<String>> ignoredModules = new HashMap<File, Set<String>>();
    private Repository repository;
    private boolean verbose;
    private boolean isDebianBuild;
    private boolean isBuildWithoutDoc;
    private boolean publishUsedRule = true;
    private boolean fixVersions = true;
    private ListOfPOMs listOfPOMs;

    public POMTransformer() {
        publishedRules.setDontDuplicate(rules);
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public ListOfPOMs getListOfPOMs() {
        return listOfPOMs;
    }

    public void setListOfPOMs(ListOfPOMs listOfPOMs) {
        this.listOfPOMs = listOfPOMs;
    }

    public boolean isBuildWithoutDoc() {
        return isBuildWithoutDoc;
    }

    public void setBuildWithoutDoc(boolean isBuildWithoutDoc) {
        this.isBuildWithoutDoc = isBuildWithoutDoc;
    }

    public boolean isDebianBuild() {
        return isDebianBuild;
    }

    public void setDebianBuild(boolean isDebianBuild) {
        this.isDebianBuild = isDebianBuild;
    }

    public boolean isPublishUsedRule() {
        return publishUsedRule;
    }

    public void setPublishUsedRule(boolean publishUsedRule) {
        this.publishUsedRule = publishUsedRule;
    }

    public boolean isFixVersions() {
        return fixVersions;
    }

    public void setFixVersions(boolean fixVersions) {
        this.fixVersions = fixVersions;
    }

    public DependencyRuleSet getRules() {
        return rules;
    }

    public DependencyRuleSet getAutomaticRules() {
        return automaticRules;
    }

    public DependencyRuleSet getPublishedRules() {
        return publishedRules;
    }

    public DependencyRuleSet getIgnoreRules() {
        return ignoreRules;
    }

    public void usePluginVersionsFromRepository() {
        repository.scanOnce();
        for (Iterator<POMInfo> i = repository.resolvedPomsIterator(); i.hasNext();) {
            POMInfo pom = i.next();
            if (pom.getThisPom().getType().equals("maven-plugin")) {
                Set<DependencyRule> pomRules = pom.getPublishedRules();
                rules.add(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
                boolean found = false;
                for (DependencyRule rule: pomRules) {
                    if (rule.matches(pom.getThisPom()) && rule.apply(pom.getThisPom()).equals(pom.getThisPom())
                            && !rule.getGroupRule().isGeneric() && !rule.getArtifactRule().isGeneric()) {
                        automaticRules.add(rule);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    DependencyRule rule = new DependencyRule(pom.getThisPom().getGroupId() + " "
                            + pom.getThisPom().getArtifactId() + " maven-plugin s/.*/"
                            + pom.getThisPom().getVersion() + "/");
                    automaticRules.add(rule);
                }
            }
        }
        // Remove the default rules from the list of automatic rules, as they may be added by the scanning
        // but addDefaultRules() may not have been called
        automaticRules.getRules().remove(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
        automaticRules.getRules().remove(DependencyRule.TO_DEBIAN_VERSION_RULE);
    }

    public void addDefaultRules() {
        addRule(DependencyRule.TO_DEBIAN_VERSION_RULE);
        addRule(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
    }

    public void addRule(DependencyRule rule) {
        rules.add(rule);
    }

    public void addPublishedRule(DependencyRule rule) {
        publishedRules.add(rule);
    }

    public void addIgnoreRule(DependencyRule rule) {
        ignoreRules.add(rule);
    }

    public void addIgnoreModule(File pomFile, String module) {
        pomFile = pomFile.getAbsoluteFile();
        Set<String> modules = ignoredModules.get(pomFile);
        if (modules == null) {
            modules = new HashSet<String>();
            ignoredModules.put(pomFile, modules);
        }
        modules.add(module);
    }

    public void discoverModulesToIgnore() {
        listOfPOMs.foreachPoms(new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent, boolean hasPackageVersion) throws Exception {
            }

            public void ignorePOM(File pomFile) throws Exception {
                File parentPom = new File(pomFile.getParentFile().getParentFile(), "pom.xml");
                addIgnoreModule(parentPom, pomFile.getParentFile().getName());
            }
        });
    }

    public void keepPomVersions() {
        listOfPOMs.foreachPoms(new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent, boolean hasPackageVersion) throws Exception {
                keepPomVersion(pomFile);
            }

            public void ignorePOM(File pomFile) throws Exception {
            }
        });
    }

    public void keepPomVersion(File pomFile) throws XMLStreamException, FileNotFoundException {
        Dependency pom = readPom(pomFile).getThisPom();
        addRule(new DependencyRule(pom.getGroupId() + " " + pom.getArtifactId() + " " + pom.getType() + " " + pom.getVersion()));
    }

    public boolean keepParentVersion(File pomFile, boolean noParent, boolean keepPomVersion) throws Exception {
        boolean keepParentVersion = !noParent && keepPomVersion;
        if (keepParentVersion && repository != null) {
            POMInfo pom = readPom(pomFile);
            if (pom.getThisPom().getType().equals("pom") && pom.getParent() != null) {
                keepParentVersion = listOfPOMs.contains(pomFile) ||
                        repository.searchMatchingPOM(pom.getParent()) == null;
            }
        }
        return keepParentVersion;
    }

    public void transformPoms(final String debianPackage, final boolean keepPomVersion,
            final String setVersion) {
        discoverModulesToIgnore();
        listOfPOMs.foreachPoms(new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent, boolean hasPackageVersion) throws Exception {
                File targetFile = new File(pomFile.getAbsolutePath() + ".new");
                boolean keepParentVersion = keepParentVersion(pomFile, noParent, keepPomVersion);
                transformPom(pomFile, targetFile, noParent, hasPackageVersion, keepPomVersion, keepParentVersion, setVersion, debianPackage);
                pomFile.delete();
                targetFile.renameTo(pomFile);
            }

            public void ignorePOM(File pomFile) throws Exception {
            }
        });
    }

    public POMInfo transformPom(File originalPom, File targetPom) throws XMLStreamException, IOException {
        return transformPom(originalPom, targetPom, false, false, false, false, null, null);
    }

    public POMInfo transformPom(File originalPom, File targetPom,
            boolean noParent, boolean hasPackageVersion, boolean keepPomVersion, boolean keepParentVersion, String setVersion, String debianPackage) throws XMLStreamException, IOException {
        return transformPom(originalPom, targetPom, noParent, hasPackageVersion, keepPomVersion, keepParentVersion, setVersion, debianPackage, false);
    }

    public POMInfo transformPom(File originalPom, File targetPom,
            boolean noParent, boolean hasPackageVersion, boolean keepPomVersion, boolean keepParentVersion, String setVersion, String debianPackage,
            boolean ignoreRegisterErrors) throws XMLStreamException, IOException {

        if (targetPom.getParentFile() != null) {
            targetPom.getParentFile().mkdirs();
        }

        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            // First pass - read information for this POM
            POMInfo original = readPom(originalPom);

            if (setVersion != null) {
                original.getThisPom().setVersion(setVersion);
            }

            if (keepParentVersion && original.getParent() != null) {
                // Add a rule to also keep the parent version
                automaticRules.add(new DependencyRule(original.getParent().getGroupId() + " "
                    + original.getParent().getArtifactId() + " * * * *"));
            }
            /*
            if (original.getParent() != null && !noParent) {
                if (repository == null && keepPomVersion) {
                    // Add a rule to also keep the parent version
                    automaticRules.add(new DependencyRule(original.getParent().getGroupId() + " "
                        + original.getParent().getArtifactId() + " * * * *"));
                } else if (repository != null) {
                    // Look in the repository and add the rules which match the parent POM
                    List parentPoms = repository.searchMatchingPOMsIgnoreVersion(original.getParent());
                    // If not found and keepPomVersion is set, in doubt keep the parent version
                    if (parentPoms.isEmpty() && keepPomVersion) {
                        automaticRules.add(new DependencyRule(original.getParent().getGroupId() + " "
                            + original.getParent().getArtifactId() + " * * * *"));
                    } else {
                        // Otherwise use the transformation rules from the repository for the parent
                        for (Iterator i = parentPoms.iterator(); i.hasNext(); ) {
                            POMInfo potentialParentPOM = (POMInfo) i.next();
                            automaticRules.addAll(potentialParentPOM.getPublishedRules(false));
                        }
                    }
                }
            } */

            transformingPom(original);

            Set<DependencyRule> allRules = new TreeSet<DependencyRule>(rules.getRules());
            allRules.addAll(automaticRules.getRules());
            POMInfo info = original.newPOMFromRules(allRules, repository);
            if (hasPackageVersion) {
                info.getProperties().put("debian.hasPackageVersion", "true");
            }
            if (noParent) {
                info.setParent(null);
            }

            if (repository != null) {
                try {
                    repository.registerPom(targetPom, info);
                } catch (DependencyNotFoundException e) {
                    if (!noParent && !ignoreRegisterErrors) {
                        System.err.println("[ERROR] Cannot find parent dependency " + e.getDependency() +
                                ", use --no-parent option to resolve this issue or install the parent POM in the Maven repository");
                    }
                }
            }

            Dependency parent = noParent ? null : info.getParent();

            // Second pass - create the new document
            int inIgnoredElement = 0;
            int inCopyOnlyElement = 0;
            int inDependency = 0;
            int inExclusion = 0;
            int inExtension = 0;
            int inPlugin = 0;
            int inProperties = 0;
            int inLevel = 0;
            int inModule = 0;
            boolean sawVersion = false;
            List<String> path = new ArrayList<String>();
            List<Dependency> dependencyList = null;
            int dependencyIndex = -1;
            Map<String, Integer> dependencyIndexes = new HashMap<String, Integer>();
            Map<String, String> visitedProperties = new HashMap<String, String>();
            Dependency dependency = null;
            Dependency parentDependency = null;
            String element = null;
            boolean afterText = false;
            XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(originalPom)));
            out = new BufferedWriter(new FileWriter(targetPom));
            XMLStreamWriter writer = outFactory.createXMLStreamWriter(out);

            writer.writeStartDocument("UTF-8", "1.0");

            for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        element = parser.getLocalName();
                        if (isWriteIgnoredElement(element, path, dependency) || (inLevel == 1 && isInfoElement(element))) {
                            inIgnoredElement++;
                            if ("version".equals(element)) {
                                sawVersion = true;
                            }
                        } else if (inLevel == 1 && "properties".equals(element) && original.getProperties().isEmpty()) {
                            inIgnoredElement++;
                        } else if (inIgnoredElement > 0) {
                            inIgnoredElement++;
                        } else if (inCopyOnlyElement > 0 | (inPlugin > 0 && ("executions".equals(element) || "configuration".equals(element)) || "goals".equals(element) || "reportSets".equals(element)) ||
                                (inDependency > 0 && "exclusions".equals(element))) {
                            inCopyOnlyElement++;

                            inLevel++;
                            path.add(element);

                            indent(writer, inLevel - 1);
                            writer.writeStartElement(element);
                            copyNsAndAttributes(parser, writer);

                        } else {
                            inLevel++;
                            path.add(element);

                            // Handle the special case of dependencies or plugins which can be ignored
                            // such as test dependencies during a clean operation
                            if ("dependency".equals(element) || "plugin".equals(element) || "extension".equals(element)) {
                                dependency = null;
                                if ("dependency".equals(element)) {
                                    String parentElement = path.get(path.size() - 2);
                                    String parentParentElement = path.get(path.size() - 3);
                                    if ("dependencies".equals(parentElement)) {
                                        sawVersion = false;
                                        String listSelector = null;
                                        if ("dependencyManagement".equals(parentParentElement)) {
                                            String p3Element = path.get(path.size() - 4);
                                            if ("project".equals(p3Element)) {
                                                listSelector = POMInfo.DEPENDENCY_MANAGEMENT_LIST;
                                            } else if ("profile".equals(p3Element)) {
                                                listSelector = POMInfo.PROFILE_DEPENDENCY_MANAGEMENT_LIST;
                                            }
                                        } else if ("project".equals(parentParentElement)) {
                                            listSelector = POMInfo.DEPENDENCIES;
                                        } else if ("profile".equals(parentParentElement)) {
                                            listSelector = POMInfo.PROFILE_DEPENDENCIES;
                                        } else if ("plugin".equals(parentParentElement)) {
                                            String p5Element = path.get(path.size() - 6);
                                            if ("project".equals(p5Element)) {
                                                listSelector = POMInfo.PLUGIN_DEPENDENCIES;
                                            } else if ("build".equals(p5Element)) {
                                                listSelector = POMInfo.PLUGIN_MANAGEMENT_DEPENDENCIES;
                                            } else if ("profile".equals(p5Element)) {
                                                listSelector = POMInfo.PROFILE_PLUGIN_DEPENDENCIES;
                                            }
                                        }
                                        if (listSelector != null) {
                                            dependencyIndex = inc(dependencyIndexes, listSelector);
                                            dependencyList = info.getDependencyList(listSelector);
                                            if (dependency != null) {
                                                parentDependency = dependency;
                                            }
                                            dependency = dependencyList.get(dependencyIndex);
                                        }
                                    }
                                } else if ("plugin".equals(element)) {
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
                                    if ("plugins".equals(parentElement)) {
                                        sawVersion = false;
                                        String listSelector = POMInfo.PLUGINS;
                                        if ("pluginManagement".equals(parentParentElement)) {
                                            if ("profile".equals(p4Element)) {
                                                listSelector = POMInfo.PROFILE_PLUGIN_MANAGEMENT;
                                            } else {
                                                listSelector = POMInfo.PLUGIN_MANAGEMENT;
                                            }
                                        } else if ("reporting".equals(parentParentElement)) {
                                            if ("profile".equals(parentParentParentElement)) {
                                                listSelector = POMInfo.PROFILE_REPORTING_PLUGINS;
                                            } else {
                                                listSelector = POMInfo.REPORTING_PLUGINS;
                                            }
                                        } else if ("profile".equals(parentParentParentElement)) {
                                            listSelector = POMInfo.PROFILE_PLUGINS;
                                        }
                                        dependencyIndex = inc(dependencyIndexes, listSelector);
                                        dependencyList = info.getDependencyList(listSelector);
                                        dependency = dependencyList.get(dependencyIndex);
                                    }
                                } else if ("extension".equals(element)) {
                                    String parentElement = path.get(path.size() - 2);
                                    if ("extensions".equals(parentElement)) {
                                        sawVersion = false;
                                        int index = inc(dependencyIndexes, POMInfo.EXTENSIONS);
                                        dependency = info.getExtensions().get(index);
                                    }
                                }
                                // Skip dependency if we can't find it (== null)
                                if (dependency == null || !acceptDependency(dependency, info)) {
                                    inIgnoredElement++;
                                    inLevel--;
                                    path.remove(path.size() - 1);
                                    dependency = null;
                                    continue;
                                }
                            }
                            if ("module".equals(element)) {
                                String parentElement = path.get(path.size() - 2);
                                String parentParentElement = path.get(path.size() - 3);
                                if ("modules".equals(parentElement) && "project".equals(parentParentElement)) {
                                    int index = inc(dependencyIndexes, POMInfo.MODULES);
                                    String module = info.getModules().get(index);
                                    if (!acceptModule(module, originalPom)) {
                                        if (verbose) {
                                          System.out.println("Ignore module " + module + " in transformed POM");
                                        }
                                        inIgnoredElement++;
                                        inLevel--;
                                        path.remove(path.size() - 1);
                                        continue;
                                    }
                                }
                            }

                            indent(writer, inLevel - 1);
                            writer.writeStartElement(element);
                            copyNsAndAttributes(parser, writer);

                            if ("project".equals(element) && inLevel == 1) {
                                copyAndFillProjectHeader(parser, writer, inLevel, keepPomVersion, info, original, parent, debianPackage);
                            } else if (inLevel == 2 && "properties".equals(element)) {
                                inProperties++;
                            } else if (inProperties > 0) {
                                visitedProperties.put(element, "true");
                                inProperties++;
                            } else if (inLevel == 2 && "modules".equals(element)) {
                                inModule++;
                            } else if (inModule > 0) {
                                inModule++;
                            } else if ("dependency".equals(element)) {
                                inDependency++;
                            } else if (inExclusion > 0) {
                                inExclusion++;
                            } else if (inDependency > 0) {
                                if ("exclusion".equals(element)) {
                                    inExclusion++;
                                } else {
                                    inDependency++;
                                }
                            } else if ("plugin".equals(element)) {
                                inPlugin++;
                            } else if (inPlugin > 0) {
                                inPlugin++;
                            } else if ("extension".equals(element)) {
                                inExtension++;
                            } else if (inExtension > 0) {
                                inExtension++;
                            }
                        }

                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        if (inIgnoredElement > 0) {
                            inIgnoredElement--;
                        } else {
                            // Attempt to repair missing version information on dependencies
                            if (dependency != null && !sawVersion && inCopyOnlyElement == 0) {
                                if ((inDependency == 1 || inPlugin == 1 || inExtension == 1)
                                        && ((parent == null && repository == null) || (repository != null && info.getVersionFromManagementDependency(dependency) == null))) {
                                    if (dependency.getVersion() == null && fixVersions) {

                                        String version = info.getVersionFromManagementDependency(dependency);
                                        if (version == null) {
                                            version = "debian";
                                        }
                                        Dependency fixedDependency = new Dependency(dependency);
                                        fixedDependency.setVersion(version);
                                        
                                        // Give a chance to customize the version
                                        // In maven.rules, you can write:
                                        // myDependencyGroup myDependencyArtifact * s/.*/myVersion/
                                        fixedDependency = fixedDependency.applyRules(rules.getRules());
                                        fixedDependency = fixedDependency.applyRules(automaticRules.getRules());
                                        dependencyList.set(dependencyIndex, fixedDependency);
                                        dependency = fixedDependency;
                                    }
                                    // If we try to fix the version for a plugin, the fix is valid
                                    // only if a real version (not 'debian') is forced on that plugin and the plugin is not
                                    // declared in any pluginManagement section in this pom or its parents
                                    // For simple dependencies, we avoid inserting the version if it's in the dependency management
                                    boolean insertVersion = (info.getVersionFromManagementDependency(dependency) == null);
                                    if (inPlugin == 1 && insertVersion) {
                                        insertVersion = !"debian".equals(dependency.getVersion());
                                    }
                                    if (insertVersion) {
                                        indent(writer, inLevel);
                                        writer.writeStartElement("version");
                                        writer.writeCharacters(dependency.getVersion());
                                        writer.writeEndElement();
                                    }
                                }
                            }

                            inLevel--;
                            path.remove(path.size() - 1);
                            if (inExclusion > 0) {
                                inExclusion--;
                            } else if (inCopyOnlyElement > 0) {
                                inCopyOnlyElement--;
                            } else if (inDependency > 0) {
                                inDependency--;
                            } else if (inPlugin > 0) {
                                inPlugin--;
                            } else if (inExtension > 0) {
                                inExtension--;
                            } else if (inModule > 0) {
                                inModule--;
                            }
                            if (inDependency + inPlugin + inExtension == 0) {
                                dependency = null;
                                dependencyIndex = -1;
                                dependencyList = null;
                                if (parentDependency != null) {
                                    dependency = parentDependency;
                                    parentDependency = null;
                                }
                            }
                            if (inProperties > 0) {
                                inProperties--;
                                if (inProperties == 0) {
                                    createDebianProperties(info, original, debianPackage, inLevel);
                                    writeMissingProperties(writer, inLevel, info, visitedProperties);
                                }
                            }
                            if (!afterText) {
                                indent(writer, inLevel);
                            }
                            writer.writeEndElement();
                            afterText = false;
                        }
                        element = null;
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        if (inIgnoredElement == 0) {
                            String value = parser.getText().trim();
                            if (dependency != null && (inDependency == 2 || inPlugin == 2 || inExtension == 2) && inExclusion == 0) {
                                if ("groupId".equals(element)) {
                                    value = dependency.getGroupId();
                                } else if ("artifactId".equals(element)) {
                                    value = dependency.getArtifactId();
                                } else if ("type".equals(element)) {
                                    value = dependency.getType();
                                } else if ("version".equals(element)) {
                                    value = dependency.getVersion();
                                    sawVersion = true;
                                }
                            } else if (inPlugin > 0 && path.contains("configuration")) {
                                if ("resourceBundle".equals(path.get(path.size() - 1))) {
                                    Matcher dependencyMatcher = compactDependencyNotationMatcher.matcher(value);
                                    if (dependencyMatcher.matches()) {
                                        Dependency embeddedDependency = new Dependency(dependencyMatcher.group(1),
                                                dependencyMatcher.group(2), "jar", dependencyMatcher.group(3));
                                        embeddedDependency = embeddedDependency.applyRules(rules.getRules());
                                        value = embeddedDependency.getGroupId() + ":" +
                                                embeddedDependency.getArtifactId() + ":" +
                                                embeddedDependency.getVersion();
                                    }
                                }
                            } else if (inProperties > 1) {
                                visitedProperties.put(element, value);
                            }
                            writer.writeCharacters(value);
                            afterText = value != null && value.length() > 0;
                        }
                        break;
                    }

                    case XMLStreamConstants.COMMENT: {
                        if (inIgnoredElement == 0) {
                            indent(writer, inLevel);
                            writer.writeComment(parser.getText());
                        }
                    }

                } // end switch
            } // end for
            parser.close();

            writer.writeEndDocument();
            writer.flush();
            writer.close();

            info.applyIgnoreRulesOnDependenciesAndPlugins(ignoreRules.getRules());

            return info;

        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }

    private void copyAndFillProjectHeader(XMLStreamReader parser, XMLStreamWriter writer, int inLevel, boolean keepPomVersion, POMInfo info, POMInfo original, Dependency parent, String debianPackage) throws XMLStreamException {
        if (parser.getNamespaceCount() == 0) {
            writer.writeNamespace(null, "http://maven.apache.org/POM/4.0.0");
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            // The correct URL is http://maven.apache.org/xsd/maven-4.0.0.xsd but
            // as gcj fails to validate it, we need to replace 4.0.0 with 4_0_0
            writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4_0_0.xsd");
        }
        indent(writer, inLevel);
        writer.writeStartElement("modelVersion");
        writer.writeCharacters("4.0.0");
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("groupId");
        writer.writeCharacters(info.getThisPom().getGroupId());
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("artifactId");
        writer.writeCharacters(info.getThisPom().getArtifactId());
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("version");
        if (keepPomVersion) {
            writer.writeCharacters(info.getOriginalVersion());
        } else {
            writer.writeCharacters(info.getThisPom().getVersion());
        }
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("packaging");
        writer.writeCharacters(info.getThisPom().getType());
        writer.writeEndElement();
        indent(writer, inLevel);
        if (parent != null) {
            writer.writeStartElement("parent");
            indent(writer, inLevel + 1);
            writer.writeStartElement("groupId");
            writer.writeCharacters(parent.getGroupId());
            writer.writeEndElement();
            indent(writer, inLevel + 1);
            writer.writeStartElement("artifactId");
            writer.writeCharacters(parent.getArtifactId());
            writer.writeEndElement();
            indent(writer, inLevel + 1);
            writer.writeStartElement("version");
            writer.writeCharacters(parent.getVersion());
            writer.writeEndElement();
            if (!isWriteIgnoredElement("relativePath", new ArrayList<String>(), null) && null != parent.getRelativePath()) {
                indent(writer, inLevel + 1);
                writer.writeStartElement("relativePath");
                writer.writeCharacters(parent.getRelativePath());
                writer.writeEndElement();
            }
            indent(writer, inLevel);
            writer.writeEndElement();
            indent(writer, inLevel);
        }
        if (original.getProperties().isEmpty()) {
            writer.writeStartElement("properties");
            createDebianProperties(info, original, debianPackage, inLevel);
            writeMissingProperties(writer, inLevel, info, new HashMap<String, String>());
            indent(writer, inLevel);
            writer.writeEndElement();
            indent(writer, inLevel);
        }
    }

    private void copyNsAndAttributes(XMLStreamReader parser, XMLStreamWriter writer) throws XMLStreamException {
        int nbNamespace = parser.getNamespaceCount();
        for (int i = 0; i < nbNamespace; i++) {
            String nsPrefix = parser.getNamespacePrefix(i);
            String nsURI = parser.getNamespaceURI(i);
            // Workaround gcj bug
            // See http://gcc.gnu.org/bugzilla/show_bug.cgi?id=40653
            try {
                if (nsPrefix == null || "".equals(nsPrefix)) {
                    writer.writeDefaultNamespace(nsURI);
                } else {
                    writer.writeNamespace(nsPrefix, nsURI);
                }
            } catch (IllegalArgumentException ignore) {
            }
        }
        int nbAttributes = parser.getAttributeCount();
        for (int i = 0; i < nbAttributes; i++) {
            String attrNamespace = parser.getAttributeNamespace(i);
            String attrPrefix = parser.getAttributePrefix(i);
            String attrName = parser.getAttributeLocalName(i);
            String value = parser.getAttributeValue(i);
            // Workaround gcj bug
            // See http://gcc.gnu.org/bugzilla/show_bug.cgi?id=40653
            if (attrNamespace == null) {
                writer.writeAttribute(attrName, value);
            } else {
                writer.writeAttribute(attrPrefix, attrNamespace, attrName, value);
            }
        }
    }

    protected void indent(XMLStreamWriter writer, int inLevel) throws XMLStreamException {
        writer.writeCharacters("\n");
        for (int i = 0; i < inLevel; i++) {
            writer.writeCharacters("\t");
        }
    }

    protected boolean isWriteIgnoredElement(String element, List<String> path, Dependency dependency) {
//        if (isDebianBuild() && DEBIAN_BUILD_IGNORED_ELEMENTS.contains(element)) {
//            System.out.println("Build ignored " + element + " " + printPath(path) + " for " + dependency);
//        }
//        if (isBuildWithoutDoc() && DEBIAN_DOC_IGNORED_ELEMENTS.contains(element)) {
//            System.out.println("Doc ignored " + element + " " + printPath(path) + " for " + dependency);
//        }
//        if (WRITE_IGNORED_ELEMENTS.contains(element)) {
//            System.out.println("Write ignored " + element + " " + printPath(path) + " for " + dependency);
//        }
        return path.size() == 1 && (isDebianBuild() && DEBIAN_BUILD_IGNORED_ELEMENTS.contains(element))
                || (isBuildWithoutDoc() && DEBIAN_DOC_IGNORED_ELEMENTS.contains(element))
                || WRITE_IGNORED_ELEMENTS.contains(element);
    }

//    String printPath(List path) {
//        StringBuilder sb = new StringBuilder();
//        for (Iterator i = path.iterator(); i.hasNext(); ) {
//            sb.append(i.next());
//            if (i.hasNext()) sb.append("/");
//        }
//        return sb.toString();
//    }

    protected boolean isInfoElement(String element) {
        return INFO_ELEMENTS.contains(element);
    }

    protected boolean acceptDependency(Dependency dependency, POMInfo info) {
        return dependency.findMatchingRule(ignoreRules.getRules()) == null;
    }

    private int inc(Map<String, Integer> dependencyIndexes, String selector) {
        Integer index = dependencyIndexes.get(selector);
        if (index == null) {
            index = 0;
        } else {
            index = index + 1;
        }
        dependencyIndexes.put(selector, index);
        return index;
    }

    protected void createDebianProperties(POMInfo info, POMInfo original, String debianPackage, int inLevel) throws XMLStreamException {
        info.getProperties().put("debian.originalVersion", info.getOriginalVersion());
        if (debianPackage != null) {
            info.getProperties().put("debian.package", debianPackage);
        }
        if (!info.getProperties().containsKey("debian.mavenRules")) {
            if (publishUsedRule && info.getOriginalPom() != null) {
                DependencyRule usedRule = info.getOriginalPom().findMatchingRule(rules.getRules());
                if (usedRule != null && !usedRule.equals(DependencyRule.TO_DEBIAN_VERSION_RULE) && !usedRule.equals(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE)) {
                    addPublishedRule(usedRule);
                }
            }
            if (!publishedRules.isEmpty()) {
                StringWriter sw = new StringWriter();
                for (Iterator i = publishedRules.iterator(); i.hasNext();) {
                    DependencyRule dependencyRule = (DependencyRule) i.next();
                    sw.append(dependencyRule.toString());
                    if (i.hasNext()) {
                        sw.append(",\n");
                        for (int j = 0; j <= inLevel; j++) {
                            sw.append("\t");
                        }
                    }
                }
                info.getProperties().put("debian.mavenRules", sw.toString());
            }
        }
    }

    protected void writeMissingProperties(XMLStreamWriter writer, int inLevel, POMInfo info, Map<String, String> visitedProperties) throws XMLStreamException {
        Map<String, String> sortedProperties = new TreeMap<String, String>(info.getProperties());
        for (Map.Entry<String, String> entry: sortedProperties.entrySet()) {
            String property = entry.getKey();
            String value = entry.getValue();
            if (!visitedProperties.containsKey(property)) {
                indent(writer, inLevel + 1);
                if (value == null || value.isEmpty() || "true".equals(value)) {
                    writer.writeEmptyElement(property);
                } else {
                    writer.writeStartElement(property);
                    writer.writeCharacters(value);
                    writer.writeEndElement();
                }
            }
        }
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        this.rules.setVerbose(verbose);
        this.automaticRules.setVerbose(verbose);
        this.publishedRules.setVerbose(verbose);
        this.ignoreRules.setVerbose(verbose);
        if (listOfPOMs != null) {
            this.listOfPOMs.setVerbose(verbose);
        }
    }

    private boolean acceptModule(String module, File pomFile) {
        Set<String> modulesToSkip = ignoredModules.get(pomFile.getAbsoluteFile());
        return modulesToSkip == null || !modulesToSkip.contains(module);
    }

    protected void transformingPom(POMInfo info) {
    }

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: Transforms the POM files to use Debian versions.");
            System.out.println("Usage: [option] <poms>");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -p<package>, --package=<package>: name of the Debian package containing");
            System.out.println("    this library");
            System.out.println("  -r<rules>, --rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to apply when cleaning the POM");
            System.out.println("  -R<rule>, --extra-rule=<rule>: extra rule to apply when cleaning the POM");
            System.out.println("    May occur multiple times, instead of or in addition to -r");
            System.out.println("  -u<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  -u<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  -U<rule>, --extra-published-rule=<rule>: extra rule to publish in debian.mavenRules");
            System.out.println("    May occur multiple times, instead of or in addition to -u");
            System.out.println("  -i<rules>, --ignore-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules use to remove certain dependencies from the transformed POM");
            System.out.println("    This option can be repeated, in order to have multiple sets of");
            System.out.println("    dependencies to ignore, useful in situations such as when the Maven clean");
            System.out.println("    target requires more dependencies or plugins to ignore than the build target");
            System.out.println("  -I<rule>, --extra-ignore-rule=<rule>: extra rule used to remove dependencies");
            System.out.println("    from the transformed POM");
            System.out.println("    May occur multiple times, instead of or in addition to -i");
            System.out.println("  --no-rules: don't apply any rules for converting versions, ");
            System.out.println("    do not even convert versions to the default 'debian' version");
            System.out.println("  --no-publish-used-rule: don't publish the rule used to transform");
            System.out.println("    a POM's own attributes in debian.mavenRules");
            System.out.println("  -e<version>, --set-version=<version>: set the version for the POM,");
            System.out.println("    do not use the version declared in the POM file.");
            System.out.println("  --keep-pom-version: keep the original version of the POMs but, ");
            System.out.println("    convert all other versions in dependencies and plugins");
            System.out.println("  --debian-build: transform during a Debian build, which means that");
            System.out.println("    some POM elements will be removed");
            System.out.println("  --build-no-docs: transform during a build where no documentation is generated,");
            System.out.println("    which means that some POM elements will be removed");
            System.out.println("  -m<repo root>--maven-repo=<repo root>: location of the Maven repository,");
            System.out.println("    used to force the versions of the Maven plugins used in the current");
            System.out.println("    POM file with the versions found in the repository");
            System.out.println("");
            System.out.println("Arguments:");
            System.out.println("  poms: location of file containing the list of POM files to process.");
            System.out.println("    Optional, defaults to debian/$package");
            System.out.println("");
            System.out.println("Description:");
            System.out.println("  Reads the file debian/$package.poms and tranform each POM file");
            System.out.println("  listed in the .poms file into a POM file using the Debian versions");
            System.out.println("  of the libraries.");
            return;
        }
        POMTransformer transformer = new POMTransformer();
        int i = inc(-1, args);
        boolean verbose = false;
        boolean noRules = false;
        boolean keepPomVersion = false;
        boolean singlePom = false;
        boolean noParent = false;
        
        String setVersion = null;
        String debianPackage = "";
        File rulesFile = null;
        File publishedRulesFile = null;
        File ignoreRulesFile = null;
        File mavenRepo = null;
        List<String> rulesExtra = new ArrayList<String>();
        List<String> publishedRulesExtra = new ArrayList<String>();
        List<String> ignoreRulesExtra = new ArrayList<String>();

        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().length() == 0)) {
            String arg = args[i].trim();
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if ("--single".equals(arg)) {
                singlePom = true;
            } else if ("--no-parent".equals(arg)) {
                noParent = true;
            } else if ("--no-rules".equals(arg)) {
                noRules = true;
            } else if ("--no-publish-used-rule".equals(arg)) {
                transformer.setPublishUsedRule(false);
            } else if ("--keep-pom-version".equals(arg)) {
                keepPomVersion = true;
            } else if (arg.equals("--debian-build")) {
                transformer.setDebianBuild(true);
            } else if (arg.equals("--build-no-docs")) {
                transformer.setBuildWithoutDoc(true);
            } else if (arg.startsWith("-p")) {
                debianPackage = arg.substring(2);
            } else if (arg.startsWith("--package=")) {
                debianPackage = arg.substring("--package=".length());
            } else if (arg.startsWith("-r")) {
                rulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--rules=")) {
                rulesFile = new File(arg.substring("--rules=".length()));
            } else if (arg.startsWith("-R")) {
                rulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-rule=")) {
                rulesExtra.add(arg.substring("--extra-rule=".length()));
            } else if (arg.startsWith("-u")) {
                publishedRulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--published-rules=")) {
                publishedRulesFile = new File(arg.substring("--published-rules=".length()));
            } else if (arg.startsWith("-U")) {
                publishedRulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-published-rule=")) {
                publishedRulesExtra.add(arg.substring("--extra-published-rule=".length()));
            } else if (arg.startsWith("-i") || arg.startsWith("--ignore-rules=")) {
                if (arg.startsWith("-i")) {
                    ignoreRulesFile = new File(arg.substring(2));
                } else {
                    ignoreRulesFile = new File(arg.substring("--ignore-rules=".length()));
                }
            } else if (arg.startsWith("-I")) {
                ignoreRulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-ignore-rule=")) {
                ignoreRulesExtra.add(arg.substring("--extra-ignore-rule=".length()));
            } else if (arg.startsWith("-e")) {
                setVersion = arg.substring(2);
            } else if (arg.startsWith("--set-version=")) {
                setVersion = arg.substring("--set-version=".length());
            } else if (arg.startsWith("-m")) {
                mavenRepo = new File(arg.substring(2));
            } else if (arg.startsWith("--maven-repo=")) {
                mavenRepo = new File(arg.substring("--maven-repo=".length()));
            }
            i = inc(i, args);
        }

        transformer.setVerbose(verbose);

        ListOfPOMs listOfPOMs;

        if (singlePom) {
            String pomPath = args[i].trim();
            listOfPOMs = new ListOfPOMs();
            ListOfPOMs.POMOptions options = listOfPOMs.addPOM(pomPath);
            options.setNoParent(noParent);
        } else {
            File poms;
            if (i + 1 < args.length) {
                poms = new File(args[i].trim());
            } else {
                poms = new File("debian/" + debianPackage + ".poms");
            }
            listOfPOMs = new ListOfPOMs(poms);
        }
        transformer.setListOfPOMs(listOfPOMs);

        if (noRules) {
            transformer.addRule(DependencyRule.NO_CHANGE_RULE);
        } else {
            if (rulesFile != null) {
                if (!rulesFile.exists()) {
                    if (verbose) {
                        System.err.println("Cannot find file: " + rulesFile);
                    }

                } else {
                    transformer.getRules().setRulesFile(rulesFile);
                }

            } else {
                System.out.println("No rules file");
            }
            transformer.getRules().addAll(rulesExtra);

            if (ignoreRulesFile != null) {
                if (ignoreRulesFile.exists()) {
                    transformer.getIgnoreRules().setRulesFile(ignoreRulesFile);
                } else {
                    System.err.println("Cannot find file: " + ignoreRulesFile);
                }
            }
            transformer.getIgnoreRules().addAll(ignoreRulesExtra);

            if (keepPomVersion) {
                transformer.keepPomVersions();
            }

            if (publishedRulesFile != null) {
                if (publishedRulesFile.exists()) {
                    transformer.getPublishedRules().setRulesFile(publishedRulesFile);
                } else {
                    System.err.println("Cannot find file: " + publishedRulesFile);
                }

            }
            transformer.getPublishedRules().addAll(publishedRulesExtra);
            
            transformer.addDefaultRules();
        }

        if (mavenRepo != null) {
            Repository repository = new Repository(mavenRepo);
            transformer.setRepository(repository);
            transformer.usePluginVersionsFromRepository();
        }

        transformer.transformPoms(debianPackage, keepPomVersion, setVersion);
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].length() == 0);
        return i;
    }
    
}
