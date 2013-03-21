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

import org.debian.maven.cliargs.ArgumentsMap;
import org.debian.maven.repo.POMInfo.DependencyType;
import org.debian.maven.util.XMLWriterWrapper;

import static org.debian.maven.repo.POMInfo.DependencyType.*;

import static org.debian.maven.repo.DependencyRuleSet.*;

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
        for (POMInfo pom : repository.getResolvedPoms().values()) {
            if (pom.getThisPom().getType().equals("maven-plugin")) {
                Set<DependencyRule> pomRules = pom.getPublishedRules();
                rules.add(MAVEN_PLUGINS_KEEP_VERSION_RULE);
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
        automaticRules.getRules().remove(MAVEN_PLUGINS_KEEP_VERSION_RULE);
        automaticRules.getRules().remove(TO_DEBIAN_VERSION_RULE);
    }

    public void addDefaultRules() {
        addRule(TO_DEBIAN_VERSION_RULE);
        addRule(MAVEN_PLUGINS_KEEP_VERSION_RULE);
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
            // Stack of the XML path currently parsed. Most deepest XML element is first in the list.
            TreePath<String> path = new TreePath<String>();
            int inIgnoredElement = 0;
            int inCopyOnlyElement = 0;
            int inDependency = 0;
            int inExclusion = 0;
            int inExtension = 0;
            int inPlugin = 0;
            int inProperties = 0;
            int inModule = 0;
            boolean sawVersion = false;
            List<Dependency> dependencyList = null;
            int dependencyIndex = -1;
            Map<DependencyType, Integer> dependencyIndexes = new HashMap<DependencyType, Integer>();
            int moduleDependencyIndex = 0;
            Map<String, String> visitedProperties = new HashMap<String, String>();
            Dependency dependency = null;
            Dependency parentDependency = null;
            String element = null;
            boolean afterText = false;
            XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(originalPom)));
            out = new BufferedWriter(new FileWriter(targetPom));
            XMLStreamWriter writer = outFactory.createXMLStreamWriter(out);
            XMLWriterWrapper writerWrapper = new XMLWriterWrapper(writer);

            writer.writeStartDocument("UTF-8", "1.0");

            for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        element = parser.getLocalName();
                        path.add(element);
                        if (isWriteIgnoredElement(element, path, dependency) || (path.size() == 2 && INFO_ELEMENTS.contains(element))) {
                            inIgnoredElement++;
                            if ("version".equals(element)) {
                                sawVersion = true;
                            }
                        } else if (path.matches("/project/properties") && original.getProperties().isEmpty()) {
                            inIgnoredElement++;
                        } else if (inIgnoredElement > 0) {
                            inIgnoredElement++;
                        } else if (inCopyOnlyElement > 0 | (inPlugin > 0 && ("executions".equals(element) || "configuration".equals(element)) || "goals".equals(element) || "reportSets".equals(element)) ||
                                (inDependency > 0 && "exclusions".equals(element))) {
                            inCopyOnlyElement++;

                            writerWrapper.indent(path.size() - 1);
                            writer.writeStartElement(element);
                            copyNsAndAttributes(parser, writer);

                        } else {
                            // Handle the special case of dependencies or plugins which can be ignored
                            // such as test dependencies during a clean operation
                            if ("dependency".equals(element) || "plugin".equals(element) || "extension".equals(element)) {
                                dependency = null;
                                if ("dependency".equals(element)) {
                                    if ("dependencies".equals(path.parent(1))) {
                                        sawVersion = false;
                                        DependencyType listSelector = null;
                                        if ("dependencyManagement".equals(path.parent(2))) {
                                            if ("project".equals(path.parent(3))) {
                                                listSelector = DEPENDENCY_MANAGEMENT_LIST;
                                            } else if ("profile".equals(path.parent(3))) {
                                                listSelector = PROFILE_DEPENDENCY_MANAGEMENT_LIST;
                                            }
                                        } else if ("project".equals(path.parent(2))) {
                                            listSelector = DEPENDENCIES;
                                        } else if ("profile".equals(path.parent(2))) {
                                            listSelector = PROFILE_DEPENDENCIES;
                                        } else if ("plugin".equals(path.parent(2))) {
                                            if ("project".equals(path.parent(5))) {
                                                listSelector = PLUGIN_DEPENDENCIES;
                                            } else if ("build".equals(path.parent(5))) {
                                                listSelector = PLUGIN_MANAGEMENT_DEPENDENCIES;
                                            } else if ("profile".equals(path.parent(5))) {
                                                listSelector = PROFILE_PLUGIN_DEPENDENCIES;
                                            }
                                        }
                                        if (listSelector != null) {
                                            dependencyIndex = inc(dependencyIndexes, listSelector);
                                            dependencyList = info.getDependencies().get(listSelector);
                                            if (dependency != null) {
                                                parentDependency = dependency;
                                            }
                                            dependency = dependencyList.get(dependencyIndex);
                                        }
                                    }
                                } else if ("plugin".equals(element)) {
                                    if ("plugins".equals(path.parent(1))) {
                                        sawVersion = false;
                                        DependencyType listSelector = PLUGINS;
                                        if (path.matches("profile/*/pluginManagement/plugins/plugin")) {
                                            listSelector = PROFILE_PLUGIN_MANAGEMENT;
                                        } else if (path.matches("pluginManagement/plugins/plugin")) {
                                            listSelector = PLUGIN_MANAGEMENT;
                                        } else if (path.matches("profile/reporting/plugins/plugin")) {
                                            listSelector = PROFILE_REPORTING_PLUGINS;
                                        } else if(path.matches("*/reporting/plugins/plugin")) {
                                            listSelector = REPORTING_PLUGINS;
                                        } else if (path.matches("profile/*/plugins/plugin")) {
                                            listSelector = PROFILE_PLUGINS;
                                        }
                                        dependencyIndex = inc(dependencyIndexes, listSelector);
                                        dependencyList = info.getDependencies().get(listSelector);
                                        dependency = dependencyList.get(dependencyIndex);
                                    }
                                } else if ("extension".equals(element)) {
                                    if ("extensions".equals(path.parent(1))) {
                                        sawVersion = false;
                                        int index = inc(dependencyIndexes, EXTENSIONS);
                                        dependency = info.getDependencies().get(EXTENSIONS).get(index);
                                    }
                                }
                                // Skip dependency if we can't find it (== null)
                                if (dependency == null || !acceptDependency(dependency, info)) {
                                    inIgnoredElement++;
                                    dependency = null;
                                    continue;
                                }
                            }
                            if (path.matches("/project/modules/module")) {
                                String module = info.getModules().get(moduleDependencyIndex);
                                ++moduleDependencyIndex;
                                if (!acceptModule(module, originalPom)) {
                                    if (verbose) {
                                      System.out.println("Ignore module " + module + " in transformed POM");
                                    }
                                    inIgnoredElement++;
                                    continue;
                                }
                            }

                            writerWrapper.indent(path.size() - 1);
                            writer.writeStartElement(element);
                            copyNsAndAttributes(parser, writer);

                            if (path.matches("/project")) {
                                copyAndFillProjectHeader(parser, writerWrapper, keepPomVersion, info, original, parent, debianPackage);
                            } else if (path.matches("/project/properties")) {
                                inProperties++;
                            } else if (inProperties > 0) {
                                visitedProperties.put(element, "true");
                                inProperties++;
                            } else if (path.matches("/project/modules")) {
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
                        path.remove();
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
                                        writerWrapper.indent(path.size());
                                        writer.writeStartElement("version");
                                        writer.writeCharacters(dependency.getVersion());
                                        writer.writeEndElement();
                                    }
                                }
                            }

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
                                    createDebianProperties(info, original, debianPackage, path.size());
                                    writeMissingProperties(writerWrapper, path.size(), info, visitedProperties);
                                }
                            }
                            if (!afterText) {
                                writerWrapper.indent(path.size());;
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
                                if ("resourceBundle".equals(path.parent(0))) {
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
                            writerWrapper.indent(path.size());
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

    protected boolean shouldWriteRelativePath() { return true; }
    
    private void copyAndFillProjectHeader(XMLStreamReader parser, XMLWriterWrapper writerWrapper, boolean keepPomVersion, POMInfo info, POMInfo original, Dependency parent, String debianPackage) throws XMLStreamException {
        XMLStreamWriter writer = writerWrapper.getWriter();
        if (parser.getNamespaceCount() == 0) {
            writer.writeNamespace(null, "http://maven.apache.org/POM/4.0.0");
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            // The correct URL is http://maven.apache.org/xsd/maven-4.0.0.xsd but
            // as gcj fails to validate it, we need to replace 4.0.0 with 4_0_0
            writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4_0_0.xsd");
        }
        writerWrapper
            .writeFilledElement("modelVersion", "4.0.0", 1)
            .writeFilledElement("groupId", info.getThisPom().getGroupId(), 1)
            .writeFilledElement("artifactId", info.getThisPom().getArtifactId(), 1)
            .writeFilledElement("version", keepPomVersion ? info.getOriginalVersion() : info.getThisPom().getVersion(), 1)
            .writeFilledElement("packaging", info.getThisPom().getType(), 1);

        if (parent != null) {
            writerWrapper.indent(1);
            writer.writeStartElement("parent");
            writerWrapper
                .writeFilledElement("groupId", parent.getGroupId(), 2)
                .writeFilledElement("artifactId", parent.getArtifactId(), 2)
                .writeFilledElement("version", parent.getVersion(), 2);

            if (shouldWriteRelativePath() && null != parent.getRelativePath()) {
                writerWrapper.writeFilledElement("relativePath", parent.getRelativePath(), 2);
            }
            writerWrapper.indent(1);
            writer.writeEndElement();
            writerWrapper.indent(1);
        }
        if (original.getProperties().isEmpty()) {
            writer.writeStartElement("properties");
            createDebianProperties(info, original, debianPackage, 1);
            writeMissingProperties(writerWrapper, 1, info, new HashMap<String, String>());
            writerWrapper.indent(1);
            writer.writeEndElement();
            writerWrapper.indent(1);
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

    protected boolean isWriteIgnoredElement(String element, TreePath<String> path, Dependency dependency) {
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

    protected boolean acceptDependency(Dependency dependency, POMInfo info) {
        return dependency.findMatchingRule(ignoreRules.getRules()) == null;
    }

    private int inc(Map<DependencyType, Integer> dependencyIndexes, DependencyType selector) {
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
                if (usedRule != null && !usedRule.equals(TO_DEBIAN_VERSION_RULE) && !usedRule.equals(MAVEN_PLUGINS_KEEP_VERSION_RULE)) {
                    addPublishedRule(usedRule);
                }
            }
            if (!publishedRules.isEmpty()) {
                StringWriter sw = new StringWriter();
                boolean first = true;
                for (DependencyRule dependencyRule : publishedRules) {
                    if (!first) {
                        sw.append(",\n");
                        for (int j = 0; j <= inLevel; j++) {
                            sw.append("\t");
                        }
                    }
                    first = false;
                    sw.append(dependencyRule.toString());
                }
                info.getProperties().put("debian.mavenRules", sw.toString());
            }
        }
    }

    protected void writeMissingProperties(XMLWriterWrapper writerWrapper, int inLevel, POMInfo info, Map<String, String> visitedProperties) throws XMLStreamException {
        XMLStreamWriter writer = writerWrapper.getWriter();
        Map<String, String> sortedProperties = new TreeMap<String, String>(info.getProperties());
        for (Map.Entry<String, String> entry: sortedProperties.entrySet()) {
            String property = entry.getKey();
            String value = entry.getValue();
            if (!visitedProperties.containsKey(property)) {
                writerWrapper.indent(inLevel + 1);
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

        ArgumentsMap argsMap = ArgumentsMap.fromArgs(args);

        boolean verbose = argsMap.getBooleanLong("verbose");
        boolean singlePom = argsMap.getBooleanLong("single");
        boolean noParent = argsMap.getBooleanLong("no-parent");
        boolean noRules = argsMap.getBooleanLong("no-rules");
        boolean keepPomVersion = argsMap.getBooleanLong("keep-pom-version");

        transformer.setPublishUsedRule(argsMap.getBooleanLong("no-publish-used-rule"));
        transformer.setDebianBuild(argsMap.getBooleanLong("debian-build"));
        transformer.setBuildWithoutDoc(argsMap.getBooleanLong("build-no-docs"));

        String debianPackage = argsMap.getValue("package", "p", "");
        File rulesFile = argsMap.getFile("rules", "r", null);
        List<String> rulesExtra = argsMap.getValueList("extra-rule", "R");
        File publishedRulesFile = argsMap.getFile("published-rules", null, null);
        List<String> publishedRulesExtra = argsMap.getValueList("extra-published-rule", "U");
        File ignoreRulesFile = argsMap.getFile("ignore-rules", "i", null);
        List<String> ignoreRulesExtra = argsMap.getValueList("extra-ignore-rule", "I");
        String setVersion = argsMap.getValue("set-version", "e", null);
        File mavenRepo = argsMap.getFile("maven-repo", "m", null);

        transformer.setVerbose(verbose);

        ListOfPOMs listOfPOMs;

        if (singlePom) {
            String pomPath = argsMap.getArguments().get(0);
            listOfPOMs = new ListOfPOMs();
            ListOfPOMs.POMOptions options = listOfPOMs.addPOM(pomPath);
            options.setNoParent(noParent);
        } else {
            String pomsFileName = argsMap.getArguments().isEmpty()
                    ? "debian/" + debianPackage + ".poms"
                    : argsMap.getArguments().get(0);
            listOfPOMs = new ListOfPOMs(new File(pomsFileName));
        }
        transformer.setListOfPOMs(listOfPOMs);

        if (noRules) {
            transformer.addRule(NO_CHANGE_RULE);
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
}
