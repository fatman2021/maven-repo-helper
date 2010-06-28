package org.debian.maven.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final List WRITE_IGNORED_ELEMENTS = Arrays.asList(new String[]{
                "modelVersion", "parent"});
    private static final List DEBIAN_BUILD_IGNORED_ELEMENTS = Arrays.asList(new String[]{
                "distributionManagement", "repositories", "pluginRepositories"});
    private static final List DEBIAN_DOC_IGNORED_ELEMENTS = Arrays.asList(new String[]{
                "reports", "reporting", "site"});
    private static final List INFO_ELEMENTS = Arrays.asList(new String[]{"groupId",
                "artifactId", "packaging", "version"});
    private Set rules = new TreeSet();
    private Set automaticRules = new TreeSet();
    private Set publishedRules = new TreeSet();
    private Set ignoreRules = new TreeSet();
    private Map ignoredModules = new HashMap();
    private Repository repository;
    private boolean verbose;
    private boolean isDebianBuild;
    private boolean isBuildWithoutDoc;

    public POMTransformer() {
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
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

    public void addPluginRulesFromRepository() {
        repository.scanOnce();
        for (Iterator i = repository.resolvedPomsIterator(); i.hasNext();) {
            POMInfo pom = (POMInfo) i.next();
            if (pom.getThisPom().getType().equals("maven-plugin")) {
                Set pomRules = pom.getPublishedRules(true);
                boolean found = false;
                for (Iterator j = pomRules.iterator(); j.hasNext();) {
                    DependencyRule rule = (DependencyRule) j.next();
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
    }

    public void addDefaultRules() {
        addRule(DependencyRule.TO_DEBIAN_VERSION_RULE);
        addRule(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
    }

    public void addRule(DependencyRule rule) {
        rules.add(rule);
    }

    public void addRules(File file) {
        if (verbose) {
            System.out.println("Add rules:");
        }
        addRules(file, rules);
    }

    public void addPublishedRule(DependencyRule rule) {
        if (verbose) {
            System.out.println("Publish rules:");
        }
        publishedRules.add(rule);
    }

    public void addPublishedRules(File file) {
        addRules(file, publishedRules);
    }

    public void addIgnoreRule(DependencyRule rule) {
        if (verbose) {
            System.out.println("Ignore rules:");
        }
        ignoreRules.add(rule);
    }

    public void addIgnoreRules(File file) {
        addRules(file, ignoreRules);
    }

    public void addIgnoreModule(File pomFile, String module) {
        Set modules = (Set) ignoredModules.get(pomFile);
        if (modules == null) {
            modules = new HashSet();
            ignoredModules.put(pomFile, modules);
        }
        modules.add(module);
    }

    private void addRules(File file, Set dest) {
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (verbose) {
                        System.out.println("  " + line);
                    }
                    dest.add(new DependencyRule(line));
                }
            }
            if (verbose) {
                System.out.println("---------");
            }

        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void keepPomVersions(File poms, final String debianPackage) {
        foreachPoms(poms, new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent) throws Exception {
                Dependency pom = readPom(pomFile).getThisPom();
                addRule(new DependencyRule(pom.getGroupId() + " " + pom.getArtifactId() + " " + pom.getType() + " " + pom.getVersion()));
            }

            public void ignorePOM(File pomFile) throws Exception {
                File parentPom = new File(pomFile.getParentFile().getParentFile(), "pom.xml");
                addIgnoreModule(parentPom, pomFile.getParentFile().getName());
            }
        });
    }

    public void keepPomVersion(File pomFile) throws XMLStreamException, FileNotFoundException {
        Dependency pom = readPom(pomFile).getThisPom();
        addRule(new DependencyRule(pom.getGroupId() + " " + pom.getArtifactId() + " " + pom.getType() + " " + pom.getVersion()));
    }

    public void transformPoms(File poms, final String debianPackage, final boolean keepPomVersion,
            final String setVersion) {
        foreachPoms(poms, new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent) throws Exception {
                File targetFile = new File(pomFile.getAbsolutePath() + ".new");
                transformPom(pomFile, targetFile, noParent, keepPomVersion, setVersion, debianPackage);
                pomFile.delete();
                targetFile.renameTo(pomFile);
            }

            public void ignorePOM(File pomFile) throws Exception {
            }
        });
    }

    public void transformPom(File pomFile, String debianPackage,
            boolean noParent, boolean keepPomVersion, String setVersion) throws XMLStreamException, FileNotFoundException, IOException {

        File targetFile = new File(pomFile.getAbsolutePath() + ".new");
        transformPom(pomFile, targetFile, noParent, keepPomVersion, setVersion, debianPackage);
        pomFile.delete();
        targetFile.renameTo(pomFile);
    }

    public void transformPom(File originalPom, File targetPom) throws XMLStreamException, FileNotFoundException, IOException {
        transformPom(originalPom, targetPom, false, false, null, null);
    }

    public POMInfo transformPom(File originalPom, File targetPom,
            boolean noParent, boolean keepPomVersion, String setVersion, String debianPackage) throws XMLStreamException, FileNotFoundException, IOException {

        if (targetPom.getParentFile() != null) {
            targetPom.getParentFile().mkdirs();
        }

        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            // First pass - read information for this POM
            POMInfo info = readPom(originalPom);

            if (setVersion != null) {
                info.getThisPom().setVersion(setVersion);
            }

            transformingPom(info);

            info = info.applyRules(rules);
            info.applyRulesOnDependenciesAndPlugins(automaticRules);

            if (repository != null) {
                repository.registerPom(targetPom, info);
            }

            Dependency parent = noParent ? null : info.getParent();
            Dependency pomInfo = info.getThisPom();

            // Second pass - create the new document
            int inIgnoredElement = 0;
            int inDependency = 0;
            int inExclusion = 0;
            int inExtension = 0;
            int inPlugin = 0;
            int inProperty = 0;
            int inLevel = 0;
            int inModule = 0;
            boolean sawVersion = false;
            List path = new ArrayList();
            Map dependencyIndexes = new HashMap();
            Dependency dependency = null;
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
                        if (isWriteIgnoredElement(element) || (inLevel == 1 && isInfoElement(element))) {
                            inIgnoredElement++;
                        } else if (inLevel == 1 && "properties".equals(element) && info.getProperties().isEmpty()) {
                            inIgnoredElement++;
                        } else if (inIgnoredElement > 0) {
                            inIgnoredElement++;
                        } else {
                            inLevel++;
                            path.add(element);

                            // Handle the special case of dependencies or plugins which can be ignored
                            // such as test dependencies during a clean operation
                            if ("dependency".equals(element) || "plugin".equals(element) || "extension".equals(element)) {
                                dependency = null;
                                if ("dependency".equals(element)) {
                                    String parentElement = (String) path.get(path.size() - 2);
                                    String parentParentElement = (String) path.get(path.size() - 3);
                                    if ("dependencies".equals(parentElement)) {
                                        sawVersion = false;
                                        String listSelector = null;
                                        if ("dependencyManagement".equals(parentParentElement)) {
                                            String p3Element = (String) path.get(path.size() - 4);
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
                                            String p5Element = (String) path.get(path.size() - 6);
                                            if ("project".equals(p5Element)) {
                                                listSelector = POMInfo.PLUGIN_DEPENDENCIES;
                                            } else if ("profile".equals(p5Element)) {
                                                listSelector = POMInfo.PROFILE_PLUGIN_DEPENDENCIES;
                                            }
                                        }
                                        if (listSelector != null) {
                                            int index = inc(dependencyIndexes, listSelector);
                                            List dependencyList = info.getDependencyList(listSelector);
                                            dependency = (Dependency) dependencyList.get(index);
                                        }
                                    }
                                } else if ("plugin".equals(element)) {
                                    String parentElement = (String) path.get(path.size() - 2);
                                    String parentParentElement = (String) path.get(path.size() - 3);
                                    if ("plugins".equals(parentElement)) {
                                        sawVersion = false;
                                        String listSelector = POMInfo.PLUGINS;
                                        if ("pluginManagement".equals(parentParentElement)) {
                                            listSelector = POMInfo.PLUGIN_MANAGEMENT;
                                        }
                                        int index = inc(dependencyIndexes, listSelector);
                                        List dependencyList = info.getDependencyList(listSelector);
                                        dependency = (Dependency) dependencyList.get(index);
                                    }
                                } else if ("extension".equals(element)) {
                                    String parentElement = (String) path.get(path.size() - 2);
                                    if ("extensions".equals(parentElement)) {
                                        sawVersion = false;
                                        int index = inc(dependencyIndexes, POMInfo.EXTENSIONS);
                                        dependency = (Dependency) info.getExtensions().get(index);
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
                                String parentElement = (String) path.get(path.size() - 2);
                                String parentParentElement = (String) path.get(path.size() - 3);
                                if ("modules".equals(parentElement) && "project".equals(parentParentElement)) {
                                    int index = inc(dependencyIndexes, POMInfo.MODULES);
                                    String module = (String) info.getModules().get(index);
                                    if (!acceptModule(module, originalPom)) {
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
                                copyAndFillProjectHeader(parser, writer, inLevel, pomInfo, keepPomVersion, info, parent, debianPackage);
                            } else if (inLevel == 2 && "properties".equals(element)) {
                                inProperty++;
                            } else if (inProperty > 0) {
                                inProperty++;
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
                            if (dependency != null && !sawVersion) {
                                if ((inDependency == 1 || inPlugin == 1 || inExtension == 1)
                                        && ((parent == null && repository == null) || (repository != null && info.getVersionFromManagementDependency(dependency) == null))) {
                                    String oldVersion = dependency.getVersion();
                                    if (oldVersion == null) {
                                        dependency.setVersion("debian");
                                        oldVersion = "debian";
                                        // Give a chance to customize the version
                                        // In maven.rules, you can write:
                                        // myDependencyGroup myDependencyArtifact * s/.*/myVersion/
                                        dependency = dependency.applyRules(rules);
                                        dependency = dependency.applyRules(automaticRules);
                                    }
                                    // If we try to fix the version for a plugin, the fix is valid
                                    // only if a real version (not 'debian') is forced on that plugin
                                    if (inPlugin != 1 || !oldVersion.equals(dependency.getVersion())) {
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
                            }
                            if (inProperty > 0) {
                                inProperty--;
                                if (inProperty == 0) {
                                    writeDebianProperties(writer, inLevel, info, debianPackage);
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
                            }
                            writer.writeCharacters(value);
                            afterText = value != null && value.length() > 0;
                        }
                        break;
                    }

                } // end switch
            } // end for
            parser.close();

            writer.writeEndDocument();
            writer.flush();
            writer.close();

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

    private void copyAndFillProjectHeader(XMLStreamReader parser, XMLStreamWriter writer, int inLevel, Dependency pomInfo, boolean keepPomVersion, POMInfo info, Dependency parent, String debianPackage) throws XMLStreamException {
        if (parser.getNamespaceCount() == 0) {
            writer.writeNamespace(null, "http://maven.apache.org/POM/4.0.0");
            writer.writeNamespace("xsi", "http://www.w3.org/2001/XMLSchema-instance");
            writer.writeAttribute("xsi", "http://www.w3.org/2001/XMLSchema-instance", "schemaLocation",
                    "http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4_0_0.xsd");
        }
        indent(writer, inLevel);
        writer.writeStartElement("modelVersion");
        writer.writeCharacters("4.0.0");
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("groupId");
        writer.writeCharacters(pomInfo.getGroupId());
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("artifactId");
        writer.writeCharacters(pomInfo.getArtifactId());
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("version");
        if (keepPomVersion) {
            writer.writeCharacters(info.getOriginalVersion());
        } else {
            writer.writeCharacters(pomInfo.getVersion());
        }
        writer.writeEndElement();
        indent(writer, inLevel);
        writer.writeStartElement("packaging");
        writer.writeCharacters(pomInfo.getType());
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
            if (keepPomVersion) {
                // use original parent version
                writer.writeCharacters(info.getOriginalParentVersion());
            } else {
                writer.writeCharacters(parent.getVersion());
            }
            writer.writeEndElement();
            indent(writer, inLevel);
            writer.writeEndElement();
            indent(writer, inLevel);
        }
        if (info.getProperties().isEmpty()) {
            writer.writeStartElement("properties");
            writeDebianProperties(writer, inLevel, info, debianPackage);
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
            if (nsPrefix == null) {
                nsPrefix = "";
            }
            writer.writeNamespace(nsPrefix, nsURI);
        }
        int nbAttributes = parser.getAttributeCount();
        for (int i = 0; i < nbAttributes; i++) {
            String attrNamespace = parser.getAttributeNamespace(i);
            String attrPrefix = parser.getAttributePrefix(i);
            String attrName = parser.getAttributeLocalName(i);
            String value = parser.getAttributeValue(i);
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

    protected boolean isWriteIgnoredElement(String element) {
        if (isDebianBuild() && DEBIAN_BUILD_IGNORED_ELEMENTS.contains(element)) {
            return true;
        }
        if (isBuildWithoutDoc() && DEBIAN_DOC_IGNORED_ELEMENTS.contains(element)) {
            return true;
        }
        return WRITE_IGNORED_ELEMENTS.contains(element);
    }

    protected boolean isInfoElement(String element) {
        return INFO_ELEMENTS.contains(element);
    }

    protected boolean acceptDependency(Dependency dependency, POMInfo info) {
        for (Iterator i = ignoreRules.iterator(); i.hasNext();) {
            DependencyRule ignoreRule = (DependencyRule) i.next();
            if (ignoreRule.matches(dependency)) {
                return false;
            }
        }
        return true;
    }

    private int inc(Map dependencyIndexes, String selector) {
        Integer index = (Integer) dependencyIndexes.get(selector);
        if (index == null) {
            index = new Integer(0);
        } else {
            index = new Integer(index.intValue() + 1);
        }
        dependencyIndexes.put(selector, index);
        return index.intValue();
    }

    private void writeDebianProperties(XMLStreamWriter writer, int inLevel, POMInfo info, String debianPackage) throws XMLStreamException {
        indent(writer, inLevel + 1);
        writer.writeStartElement("debian.originalVersion");
        writer.writeCharacters(info.getOriginalVersion());
        writer.writeEndElement();
        if (debianPackage != null) {
            indent(writer, inLevel + 1);
            writer.writeStartElement("debian.package");
            writer.writeCharacters(debianPackage);
            writer.writeEndElement();
        }
        if (info.getOriginalPom() != null) {
            DependencyRule usedRule = info.getOriginalPom().findMatchingRule(rules);
            if (usedRule != null && !usedRule.equals(DependencyRule.TO_DEBIAN_VERSION_RULE) && !usedRule.equals(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE)) {
                addPublishedRule(usedRule);
            }
        }
        if (!publishedRules.isEmpty()) {
            indent(writer, inLevel + 1);
            writer.writeStartElement("debian.mavenRules");
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
            writer.writeCData(sw.toString());
            writer.writeEndElement();
        }
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    private boolean acceptModule(String module, File pomFile) {
        Set modulesToSkip = (Set) ignoredModules.get(pomFile);
        if (modulesToSkip == null) {
            return true;
        }
        return !modulesToSkip.contains(module);
    }

    protected void transformingPom(POMInfo info) {
    }

    private interface POMHandler {

        void handlePOM(File pomFile, boolean noParent) throws Exception;

        void ignorePOM(File pomFile) throws Exception;
    }

    private static void foreachPoms(File poms, POMHandler handler) {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(poms));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (!st.hasMoreTokens() || line.startsWith("#")) {
                    continue;
                }
                String pom = st.nextToken();
                File pomFile = new File(pom);
                while (st.hasMoreTokens()) {
                    String option = st.nextToken();
                    if ("--ignore".equals(option)) {
                        handler.ignorePOM(pomFile);
                        break;
                    }
                }
            }

            reader = new LineNumberReader(new FileReader(poms));
            newline:
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (!st.hasMoreTokens() || line.startsWith("#")) {
                    continue;
                }
                String pom = st.nextToken();
                File pomFile = new File(pom);
                boolean noParent = false;
                while (st.hasMoreTokens()) {
                    String option = st.nextToken();
                    if ("--no-parent".equals(option)) {
                        noParent = true;
                    } else if ("--ignore".equals(option)) {
                        break newline;
                    }
                }
                handler.handlePOM(pomFile, noParent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static Map getPomOptions(File poms) {
        final Map pomOptions = new HashMap();
        foreachPoms(poms, new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent) throws Exception {
                String option = "";
                if (noParent) {
                    option = "--no-parent";
                }
                pomOptions.put(pomFile.getAbsoluteFile(), option);
            }

            public void ignorePOM(File pomFile) throws Exception {
                pomOptions.put(pomFile.getAbsoluteFile(), "--ignore");
            }
        });

        return pomOptions;
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
            System.out.println("  -u<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  -u<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  -i<rules>, --ignore-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules use to remove certain dependencies from the transformed POM");
            System.out.println("    This option can be repeated, in order to have multiple sets of");
            System.out.println("    dependencies to ignore, useful in situations such as when the Maven clean");
            System.out.println("    target requires more dependencies or plugins to ignore than the build target");
            System.out.println("  --no-rules: don't apply any rules for converting versions, ");
            System.out.println("    do not even convert versions to the default 'debian' version");
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
            } else if (arg.startsWith("-u")) {
                publishedRulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--published-rules=")) {
                publishedRulesFile = new File(arg.substring("--published-rules=".length()));
            } else if (arg.startsWith("-i") || arg.startsWith("--ignore-rules=")) {
                if (arg.startsWith("-i")) {
                    ignoreRulesFile = new File(arg.substring(2));
                } else {
                    ignoreRulesFile = new File(arg.substring("--ignore-rules=".length()));
                }
                if (ignoreRulesFile.exists()) {
                    transformer.addIgnoreRules(ignoreRulesFile);
                } else {
                    System.err.println("Cannot find file: " + ignoreRulesFile);
                }

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

        File poms = null;
        File pom = null;

        if (singlePom) {
            pom = new File(args[i++].trim());
        } else if (i + 1 < args.length) {
            poms = new File(args[i++].trim());
        } else {
            poms = new File("debian/" + debianPackage + ".poms");
        }

        if (noRules) {
            transformer.addRule(DependencyRule.NO_CHANGE_RULE);
        } else {
            transformer.addDefaultRules();
            if (rulesFile != null) {
                if (!rulesFile.exists()) {
                    if (verbose) {
                        System.err.println("Cannot find file: " + rulesFile);
                    }

                } else {
                    transformer.addRules(rulesFile);
                }

            } else {
                System.out.println("No rules file");
            }

            if (keepPomVersion) {
                if (singlePom) {
                    try {
                        transformer.keepPomVersion(pom);
                    } catch (XMLStreamException e) {
                        e.printStackTrace();
                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    }
                } else {
                    transformer.keepPomVersions(poms, debianPackage);
                }
            }

            if (publishedRulesFile != null) {
                if (publishedRulesFile.exists()) {
                    transformer.addPublishedRules(publishedRulesFile);
                } else {
                    System.err.println("Cannot find file: " + publishedRulesFile);
                }

            }
        }

        if (mavenRepo != null) {
            Repository repository = new Repository(mavenRepo);
            transformer.setRepository(repository);
            transformer.addPluginRulesFromRepository();
        }

        if (singlePom) {
            try {
                transformer.transformPom(pom, debianPackage, noParent, keepPomVersion, setVersion);
            } catch (XMLStreamException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            transformer.transformPoms(poms, debianPackage, keepPomVersion, setVersion);
        }
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].length() == 0);
        return i;
    }
}
