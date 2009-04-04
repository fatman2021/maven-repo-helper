package org.debian.maven.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Cleans up a POM for inclusion in the /usr/share/maven-repo/ repository.
 *
 * All POMs should contain only 
 *
 * @author Ludovic Claude
 */
public class POMCleaner {

    private static final Logger log = Logger.getLogger(POMCleaner.class.getName());
    private static final List IGNORED_ELEMENTS = Arrays.asList(new String[]{"build",
                "distributionManagement", "profiles", "ciManagement", "prerequisites",
                "repositories", "pluginRepositories", "reports", "reporting"});
    private static final List INFO_ELEMENTS = Arrays.asList(new String[]{"groupId",
                "artifactId", "packaging", "version"});
    private final XMLInputFactory factory = XMLInputFactory.newInstance();

    public void cleanPom(File originalPom, File targetPom, File pomProperties, File versionProperties,
            boolean inheritFromDebian, Collection specialCases) {

        targetPom.getParentFile().mkdirs();
        pomProperties.getParentFile().mkdirs();
        versionProperties.getParentFile().mkdirs();

        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            // First pass - read information for this POM
            Dependency pomInfo = readPOMInfo(originalPom);
            // Second pass - collect version and parent information
            List dependencies = readDependencies(originalPom, inheritFromDebian);

            // Third pass - create the new document
            int inIgnoredElement = 0;
            int inParent = 0;
            int inDependency = 0;
            int inLevel = 0;
            int dependencyIndex = -1;
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
                        if (isIgnoredElement(element, inheritFromDebian) || inIgnoredElement > 0 || "modelVersion".equals(element) || (inDependency == 0 && inParent == 0 && isInfoElement(element))) {
                            inIgnoredElement++;
                        } else {
                            indent(writer, inLevel);
                            writer.writeStartElement(element);
                            inLevel++;

                            if ("project".equals(element) && inLevel == 1) {
                                indent(writer, inLevel);
                                if (inheritFromDebian) {
                                    writer.writeStartElement("parent");
                                    indent(writer, inLevel + 1);
                                    writer.writeStartElement("groupId");
                                    writer.writeCharacters("org.debian");
                                    writer.writeEndElement();
                                    indent(writer, inLevel + 1);
                                    writer.writeStartElement("artifactId");
                                    writer.writeCharacters("debian-parent");
                                    writer.writeEndElement();
                                    indent(writer, inLevel + 1);
                                    writer.writeStartElement("version");
                                    writer.writeCharacters("1.0-SNAPSHOT");
                                    writer.writeEndElement();
                                    indent(writer, inLevel);
                                    writer.writeEndElement();
                                    indent(writer, inLevel);
                                }
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
                                writer.writeCharacters(pomInfo.getVersion());
                                writer.writeEndElement();
                                indent(writer, inLevel);
                                writer.writeStartElement("packaging");
                                writer.writeCharacters(pomInfo.getType());
                                writer.writeEndElement();
                                indent(writer, inLevel);
                            } else if ("parent".equals(element)) {
                                inParent++;
                            } else if (inParent > 0) {
                                inParent++;
                            } else if ("dependency".equals(element)) {
                                inDependency++;
                                dependencyIndex++;
                            } else if (inDependency > 0) {
                                inDependency++;
                            }
                        }
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        if (inIgnoredElement > 0) {
                            inIgnoredElement--;
                        } else {
                            inLevel--;
                            if (inDependency > 0) {
                                inDependency--;
                            }
                            if (inParent > 0) {
                                inParent--;
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
                            if (inDependency > 0 && "version".equals(element)) {
                                Dependency dependency = (Dependency) dependencies.get(dependencyIndex);
                                value = "${" + versionProperty(dependency, specialCases) + "}";
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

            Properties pomProps = new Properties();
            pomProps.put("GROUP_ID", pomInfo.getGroupId());
            pomProps.put("ARTIFACT_ID", pomInfo.getArtifactId());
            pomProps.put("TYPE", pomInfo.getType());
            pomProps.put("VERSION", pomInfo.getVersion());
            FileWriter pomWriter = new FileWriter(pomProperties);
            pomProps.store(pomWriter, "POM properties");
            pomWriter.close();

            Properties versions = new Properties();
            if (versionProperties.exists()) {
                versions.load(new FileReader(versionProperties));
                versionProperties.delete();
            }
            versions.put(versionProperty(pomInfo, specialCases), pomInfo.getVersion());
            FileWriter versionsWriter = new FileWriter(versionProperties);
            versions.store(versionsWriter, "Version properties");
            versionsWriter.close();

        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        } catch (XMLStreamException ex) {
            log.log(Level.SEVERE, null, ex);
            return;
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                log.log(Level.SEVERE, null, ex);
                return;
            }
        }
    }

    protected Dependency readPOMInfo(File pom) throws XMLStreamException, FileNotFoundException {
        XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(pom)));
        Dependency dependency = new Dependency(null, null, "jar", null);
        int inIgnoredElement = 0;
        int inParent = 0;
        String element = null;

        // First pass - collect version and parent information
        for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    element = parser.getLocalName();
                    if (inIgnoredElement == 0 && "parent".equals(element)) {
                        inParent++;
                    } else if (inIgnoredElement == 0 && isInfoElement(element)) {
                        if (inParent > 0) {
                            inParent++;
                        }
                    } else if (!"project".equals(element)) {
                        inIgnoredElement++;
                    }
                    break;
                }

                case XMLStreamConstants.END_ELEMENT: {
                    if (inIgnoredElement > 0) {
                        inIgnoredElement--;
                    } else {
                        if (inParent > 0) {
                            inParent--;
                        }
                    }
                    element = null;
                    break;
                }

                case XMLStreamConstants.CHARACTERS: {
                    String value = parser.getText().trim();
                    if (inIgnoredElement == 0) {
                        if ("groupId".equals(element)) {
                            if (inParent == 0 || dependency.getGroupId() == null) {
                                dependency.setGroupId(value);
                            }
                        } else if ("artifactId".equals(element)) {
                            if (inParent == 0 || dependency.getArtifactId() == null) {
                                dependency.setArtifactId(value);
                            }
                        } else if ("version".equals(element)) {
                            if (inParent == 0 || dependency.getVersion() == null) {
                                dependency.setVersion(value);
                            }
                        } else if ("packaging".equals(element)) {
                            if (inParent == 0) {
                                dependency.setType(value);
                            }
                        }
                    }
                    break;
                }

            } // end switch
        } // end for
        parser.close();

        return dependency;
    }

    protected List readDependencies(File originalPom, boolean inheritFromDebian) throws XMLStreamException, FileNotFoundException {
        XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(originalPom)));
        List dependencies = new ArrayList();
        int inIgnoredElement = 0;
        int inDependency = 0;
        String element = null;

        // First pass - collect version and parent information
        for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
            switch (event) {
                case XMLStreamConstants.START_ELEMENT: {
                    element = parser.getLocalName();
                    if (isIgnoredElement(element, inheritFromDebian) || inIgnoredElement > 0) {
                        inIgnoredElement++;
                    } else {
                        if ("dependency".equals(element)) {
                            inDependency++;
                            dependencies.add(new Dependency(null, null, "jar", null));
                        } else if (inDependency > 0) {
                            inDependency++;
                        }
                    }
                    break;
                }

                case XMLStreamConstants.END_ELEMENT: {
                    if (inIgnoredElement > 0) {
                        inIgnoredElement--;
                    } else {
                        if (inDependency > 0) {
                            inDependency--;
                        }
                    }
                    element = null;
                    break;
                }

                case XMLStreamConstants.CHARACTERS: {
                    String value = parser.getText().trim();
                    if (inDependency > 0) {
                        Dependency dependency = (Dependency) dependencies.get(dependencies.size() - 1);
                        if ("groupId".equals(element)) {
                            dependency.setGroupId(value);
                        } else if ("artifactId".equals(element)) {
                            dependency.setArtifactId(value);
                        } else if ("type".equals(element)) {
                            dependency.setType(value);
                        } else if ("version".equals(element)) {
                            dependency.setVersion(value);
                        }
                    }
                    break;
                }

            } // end switch
        } // end for
        parser.close();

        return dependencies;
    }

    protected void indent(XMLStreamWriter writer, int inLevel) throws XMLStreamException {
        writer.writeCharacters("\n");
        for (int i = 0; i < inLevel; i++) {
            writer.writeCharacters("\t");
        }
    }

    protected boolean isIgnoredElement(String element, boolean inheritFromDebian) {
        if (inheritFromDebian && "parent".equals(element)) {
            return true;
        }
        return IGNORED_ELEMENTS.contains(element);
    }

    protected boolean isInfoElement(String element) {
        return INFO_ELEMENTS.contains(element);
    }

    protected String versionProperty(Dependency dependency, Collection specialCases) {
        for (Iterator i = specialCases.iterator(); i.hasNext();) {
            Dependency specialCase = (Dependency) i.next();
            if (matches(specialCase.getGroupId(), dependency.getGroupId()) &&
                    matches(specialCase.getArtifactId(), dependency.getArtifactId()) &&
                    matches(specialCase.getType(), dependency.getType())) {
                String groupId = specialCase.getGroupId().replace("*", "__");
                String artifactId = specialCase.getArtifactId().replace("*", "__");
                String type = specialCase.getType().replace("*", "__");
                String version = specialCase.getVersion().replace("*", "__");
                return groupId + "." + artifactId + "." + type + "." + version + ".version";
            }
        }
        String groupId = dependency.getGroupId();
        String artifactId = dependency.getArtifactId();
        String type = dependency.getType();
        if (type == null) {
            type = "jar";
        }
        return groupId + "." + artifactId + "." + type + ".debian.version";
    }

    private boolean matches(String reference, String test) {
        String regex = reference.replace(".", "\\.").replace("*", "[\\w\\d-]*").replace("__", "[\\w\\d-]*");
        return test.matches(regex);
    }

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: cleans a Maven POM for inclusion in the Debian/Maven repository in /usr/share/maven-repo");
            System.out.println("Usage: [option] original-pom target-file versions-properties [special-dependencies]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -d, --debian-parent: target POM will inherit directly from the Debian parent POM");
            System.out.println("");
            System.out.println("Arguments:");
            System.out.println("  original-pom: location of the original POM");
            System.out.println("  target-file: where the cleaned POM will be written to");
            System.out.println("  versions-properties: the version for this POM will be added to this properties file");
            System.out.println("  special-dependencies: optional file containing the list of special cases");
            System.out.println("    for transforming dependency versions into properties");
            System.out.println("");
            System.out.println("Description:");
            System.out.println("  Cleans a Maven POM and prepare it for inclusion in the Debian");
            System.out.println("  repository for Maven.");
            System.out.println();
            System.out.println("  POMs present in this repository should always inherit either");
            System.out.println("  directly or indirectly from the parent POM");
            System.out.println("  (org.debian:debian-parent:1.0-SNAPSHOT).");
            System.out.println("  Build, profiles and other build time only sections of the POM");
            System.out.println("  should be stripped.");
            System.out.println();
            System.out.println("  If versions are given for a dependency, this version will be");
            System.out.println("  replaced by a property of the form");
            System.out.println("  [groupId].[artifactId].[type].[debian version].version");
            System.out.println("  By default, groupId is the current groupId, idem for");
            System.out.println("  artifactId and type (jar/war/ear/ejb/pom...) and debian version");
            System.out.println("  is 'debian'");
            System.out.println();
            System.out.println("  You can modify those defaults with the help of the");
            System.out.println("  special-dependencies file. This file should contain the following lines:");
            System.out.println("  [groupId].[artifactId].[type].[debian version]");
            System.out.println("  where groupId, artifactId and type can be the explicit attribute to");
            System.out.println("  match, or can contain a wildcard (either * or __ ) in order to use");
            System.out.println("  one version property for several related dependencies.");
            System.out.println();
            System.out.println("  The dependency version is never matched.");
            System.out.println("  The Debian version is usually 'debian', but can be anything else if");
            System.out.println("  you need to have several versions of the same library in the Maven");
            System.out.println("  repository.");
            return;
        }
        POMCleaner cleaner = new POMCleaner();
        Collection specialCases = new ArrayList();
        int i = inc(-1, args);
        boolean verbose = false;
        if ("--verbose".equals(args[0]) || "-v".equals(args[0])) {
            verbose = true;
            i = inc(i, args);
        }
        boolean inheritFromDebian = false;
        if ("--debian-parent".equals(args[0]) || "-d".equals(args[0])) {
            inheritFromDebian = true;
            i = inc(i, args);
        }
        File originalPom = new File(args[i++]);
        File targetPom = new File(args[i++]);
        File pomProperties = new File(args[i++]);
        File versionProperties = new File(args[i++]);
        if (i < args.length) {
            String specialDependencies = args[i++];
            File sdFile = new File(specialDependencies);
            if (!sdFile.exists()) {
                if (verbose) {
                    System.err.println("Cannot find file: " + sdFile);
                }
            } else {
                try {
                    LineNumberReader lnr = new LineNumberReader(new FileReader(sdFile));
                    String line = null;
                    while ((line = lnr.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) {
                            break;
                        }
                        int dot = line.lastIndexOf('.');
                        String version = line.substring(dot + 1);
                        line = line.substring(0, dot);
                        dot = line.lastIndexOf('.');
                        String type = line.substring(dot + 1);
                        line = line.substring(0, dot);
                        dot = line.lastIndexOf('.');
                        String artifactId = line.substring(dot + 1);
                        String groupId = line.substring(0, dot);
                        specialCases.add(new Dependency(groupId, artifactId, type, version));
                    }

                } catch (IOException ex) {
                    log.log(Level.SEVERE, null, ex);
                }
            }
        }

        cleaner.cleanPom(originalPom, targetPom, pomProperties, versionProperties, inheritFromDebian, specialCases);
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }
}
