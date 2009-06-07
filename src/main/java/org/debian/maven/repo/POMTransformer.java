/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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
    private static final List INFO_ELEMENTS = Arrays.asList(new String[]{"groupId",
                "artifactId", "packaging", "version"});
    private Set rules = new TreeSet();
    private Set publishedRules = new TreeSet();
    private boolean verbose;

    public POMTransformer() {
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

    private void addRules(File file, Set dest) {
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(file));
            String line = null;
            while ((line = lnr.readLine()) != null) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
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
        });
    }

    public void transformPoms(File poms, final String debianPackage, final boolean keepPomVersion) {
        foreachPoms(poms, new POMHandler() {

            public void handlePOM(File pomFile, boolean noParent) throws Exception {
                File targetFile = new File(pomFile.getAbsolutePath() + ".new");
                transformPom(pomFile, targetFile, noParent, keepPomVersion, debianPackage);
                pomFile.delete();
                targetFile.renameTo(pomFile);
            }
        });
    }

    public void transformPom(File originalPom, File targetPom) throws XMLStreamException, FileNotFoundException, IOException {
        transformPom(originalPom, targetPom, false, false, null);
    }

    public POMInfo transformPom(File originalPom, File targetPom,
            boolean noParent, boolean keepPomVersion, String debianPackage) throws XMLStreamException, FileNotFoundException, IOException {

        if (targetPom.getParentFile() != null) {
            targetPom.getParentFile().mkdirs();
        }

        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            // First pass - read information for this POM
            POMInfo info = readPom(originalPom);

            info = info.applyRules(rules);
            Dependency parent = info.getParent();
            Dependency pomInfo = info.getThisPom();

            // Second pass - create the new document
            int inIgnoredElement = 0;
            int inDependency = 0;
            int inExclusion = 0;
            int inExtension = 0;
            int inPlugin = 0;
            int inProperty = 0;
            int inLevel = 0;
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
                            indent(writer, inLevel);
                            writer.writeStartElement(element);
                            inLevel++;
                            path.add(element);

                            if ("project".equals(element) && inLevel == 1) {
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
                                if (!noParent && parent != null) {
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
                            } else if (inLevel == 2 && "properties".equals(element)) {
                                inProperty++;
                            } else if (inProperty > 0) {
                                inProperty++;
                            } else if ("dependency".equals(element)) {
                                inDependency++;
                                String parentElement = (String) path.get(path.size() - 2);
                                String parentParentElement = (String) path.get(path.size() - 3);
                                if ("dependencies".equals(parentElement)) {
                                    List dependencyList = null;
                                    if ("dependencyManagement".equals(parentParentElement)) {
                                        String p3Element = (String) path.get(path.size() - 4);
                                        if ("project".equals(p3Element)) {
                                            dependencyList = info.getDependencyManagement();
                                        } else if ("profile".equals(p3Element)) {
                                            dependencyList = info.getProfileDependencyManagement();
                                        }
                                    } else if ("project".equals(parentParentElement)) {
                                        dependencyList = info.getDependencies();
                                    } else if ("profile".equals(parentParentElement)) {
                                        dependencyList = info.getProfileDependencies();
                                    }
                                    if (dependencyList != null) {
                                        int index = inc(dependencyIndexes, dependencyList);
                                        dependency = (Dependency) dependencyList.get(index);
                                    }
                                }
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
                                String parentElement = (String) path.get(path.size() - 2);
                                String parentParentElement = (String) path.get(path.size() - 3);
                                if ("plugins".equals(parentElement)) {
                                    List dependencyList;
                                    if ("pluginManagement".equals(parentParentElement)) {
                                        dependencyList = info.getPluginManagement();
                                    } else {
                                        dependencyList = info.getPlugins();
                                    }
                                    int index = inc(dependencyIndexes, dependencyList);
                                    dependency = (Dependency) dependencyList.get(index);
                                }
                            } else if (inPlugin > 0) {
                                inPlugin++;
                            } else if ("extension".equals(element)) {
                                inExtension++;
                                int index = inc(dependencyIndexes, info.getExtensions());
                                dependency = (Dependency) info.getExtensions().get(index);
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
                            if (dependency != null && inExclusion == 0) {
                                if ("groupId".equals(element)) {
                                    value = dependency.getGroupId();
                                } else if ("artifactId".equals(element)) {
                                    value = dependency.getArtifactId();
                                } else if ("type".equals(element)) {
                                    value = dependency.getType();
                                } else if ("version".equals(element)) {
                                    value = dependency.getVersion();
                                }
                            }
                            writer.writeCharacters(value);
                            afterText = !value.isEmpty();
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

    protected void indent(XMLStreamWriter writer, int inLevel) throws XMLStreamException {
        writer.writeCharacters("\n");
        for (int i = 0; i < inLevel; i++) {
            writer.writeCharacters("\t");
        }
    }

    protected boolean isWriteIgnoredElement(String element) {
        return WRITE_IGNORED_ELEMENTS.contains(element);
    }

    protected boolean isInfoElement(String element) {
        return INFO_ELEMENTS.contains(element);
    }

    private int inc(Map dependencyIndexes, List dependencyList) {
        Integer index = (Integer) dependencyIndexes.get(dependencyList);
        if (index == null) {
            index = new Integer(0);
        } else {
            index = new Integer(index.intValue() + 1);
        }
        dependencyIndexes.put(dependencyList, index);
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
        DependencyRule usedRule = info.getOriginalPom().findMatchingRule(rules);
        if (usedRule != null && !usedRule.equals(DependencyRule.TO_DEBIAN_VERSION_RULE) && !usedRule.equals(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE)) {
            addPublishedRule(usedRule);
        }
        if (!publishedRules.isEmpty()) {
            indent(writer, inLevel + 1);
            writer.writeStartElement("debian.mavenRule");
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

    private interface POMHandler {

        void handlePOM(File pomFile, boolean noParent) throws Exception;
    }

    private void foreachPoms(File poms, POMHandler handler) {
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(poms));
            String line;
            while ((line = reader.readLine()) != null) {
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (!st.hasMoreTokens()) {
                    continue;
                }
                String pom = st.nextToken();
                File pomFile = new File(pom);
                boolean noParent = false;
                if (st.hasMoreTokens()) {
                    if ("--no-parent".equals(st.nextToken())) {
                        noParent = true;
                    }
                }
                handler.handlePOM(pomFile, noParent);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
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
            System.out.println("  --no-rules: don't apply any rules for converting versions, ");
            System.out.println("    do not even convert versions to the default 'debian' version");
            System.out.println("  --keep-pom-version: keep the original version of the POMs but, ");
            System.out.println("    convert all other versions in dependencies and plugins");
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
        String debianPackage = "";
        File rulesFile = null;
        File publishedRulesFile = new File("debian/maven.publishedRules");
        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().isEmpty())) {
            String arg = args[i].trim();
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if ("--no-rules".equals(arg)) {
                noRules = true;
            } else if ("--keep-pom-version".equals(arg)) {
                keepPomVersion = true;
            } else if (arg.startsWith("-p")) {
                debianPackage = arg.substring(2);
            } else if (arg.startsWith("--package=")) {
                debianPackage = arg.substring("--package=".length());
            } else if (arg.startsWith("-r")) {
                rulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--rules=")) {
                rulesFile = new File(arg.substring("--rules=".length()));
            }
            i = inc(i, args);
        }
        transformer.setVerbose(verbose);

        File poms;
        if (i + 1 < args.length) {
            poms = new File(args[i++].trim());
        } else {
            poms = new File("debian/" + debianPackage + ".poms");
        }

        if (!noRules) {
            transformer.addDefaultRules();
            if (rulesFile != null) {
                if (!rulesFile.exists()) {
                    if (verbose) {
                        System.err.println("Cannot find file: " + rulesFile);
                    }
                } else {
                    transformer.addRules(rulesFile);
                }
            }
            if (keepPomVersion) {
                transformer.keepPomVersions(poms, debianPackage);
            }
            if (publishedRulesFile != null && publishedRulesFile.exists()) {
                transformer.addPublishedRules(publishedRulesFile);
            }
        }

        transformer.transformPoms(poms, debianPackage, keepPomVersion);
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }
}
