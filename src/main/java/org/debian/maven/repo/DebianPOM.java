package org.debian.maven.repo;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * Manages the Debian POM.
 *
 * Can add or remove library properties to that POM and create it if it did not
 * exist before.
 *
 * @author Ludovic Claude
 */
public class DebianPOM {

    private static final Logger log = Logger.getLogger(DebianPOM.class.getName());

    public void addLibrary(File pom, File updatedPom, Map libraryProperties) {
        Map remainingProperties = new TreeMap(libraryProperties);
        final XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            out = new BufferedWriter(new FileWriter(updatedPom));
            XMLStreamWriter writer = outFactory.createXMLStreamWriter(out);

            if (!pom.exists()) {
                pom.getParentFile().mkdirs();
                writer.writeStartDocument("UTF-8", "1.0");
                writer.writeCharacters("\n");
                writer.writeStartElement("project");
                writer.writeCharacters("\n\t");
                writer.writeStartElement("groupId");
                writer.writeCharacters("org.debian");
                writer.writeEndElement();
                writer.writeCharacters("\n\t");
                writer.writeStartElement("artifactId");
                writer.writeCharacters("debian-parent");
                writer.writeEndElement();
                writer.writeCharacters("\n\t");
                writer.writeStartElement("version");
                writer.writeCharacters("1.0-SNAPSHOT");
                writer.writeEndElement();
                writer.writeCharacters("\n\t");
                writer.writeStartElement("properties");
            } else {

                final XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(pom)));
                int inProperties = 0;
                String property = null;

                writer.writeStartDocument("UTF-8", "1.0");

                copy:
                for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
                    switch (event) {
                        case XMLStreamConstants.START_ELEMENT: {
                            String element = parser.getLocalName();
                            if ("properties".equals(element)) {
                                inProperties++;
                            } else if (inProperties > 0) {
                                property = element;
                            }
                            writer.writeStartElement(element);
                            break;
                        }

                        case XMLStreamConstants.END_ELEMENT: {
                            String element = parser.getLocalName();
                            if (isDeclaration(element)) {
                                writer.writeEndElement();
                            } else if ("properties".equals(element)) {
                                inProperties--;
                                break copy;
                            } else if (inProperties > 0 && property != null) {
                                writer.writeEndElement();
                                property = null;
                            }
                            break;
                        }

                        case XMLStreamConstants.CHARACTERS: {
                            String value = parser.getText();
                            if (inProperties == 0) {
                                writer.writeCharacters(value);
                            } else if (property != null) {
                                if (remainingProperties.containsKey(property)) {
                                    value = (String) remainingProperties.get(property);
                                    remainingProperties.remove(property);
                                }
                                writer.writeCharacters(value);
                            } else if ("\n\t\t".equals(value)) {
                                // After end of a property, but not before end of
                                // <properties> tag.
                                writer.writeCharacters(value);
                            }
                            break;
                        }
                    } // end switch
                } // end for
                parser.close();
            }

            for (Iterator i = remainingProperties.entrySet().iterator(); i.hasNext(); ) {
                Map.Entry entry = (Map.Entry)i.next();
                writer.writeCharacters("\n\t\t");
                writer.writeStartElement((String) entry.getKey());
                writer.writeCharacters((String) entry.getValue());
                writer.writeEndElement();
            }

            writer.writeCharacters("\n\t");
            writer.writeEndElement();
            writer.writeCharacters("\n");
            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO Error", ex);
            return;
        } catch (XMLStreamException ex) {
            log.log(Level.SEVERE, "Error when reading the XML document", ex);
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

        if (updatedPom.exists()) {
            pom.delete();
            if (!updatedPom.renameTo(pom)) {
                System.err.println("Cannot update " + pom.getAbsolutePath());
            }
        }
    }

    public void removeLibrary(File pom, File updatedPom, String oldProperty1, String oldProperty2) {
        if (!pom.exists()) {
            return;
        }

        final XMLInputFactory factory = XMLInputFactory.newInstance();
        XMLOutputFactory outFactory = XMLOutputFactory.newInstance();
        Writer out = null;
        try {
            out = new BufferedWriter(new FileWriter(updatedPom));
            XMLStreamWriter writer = outFactory.createXMLStreamWriter(out);

            final XMLStreamReader parser = factory.createXMLStreamReader(new BufferedReader(new FileReader(pom)));
            int inProperties = 0;
            String property = null;

            writer.writeStartDocument("UTF-8", "1.0");

            for (int event = parser.next(); event != XMLStreamConstants.END_DOCUMENT; event = parser.next()) {
                switch (event) {
                    case XMLStreamConstants.START_ELEMENT: {
                        String element = parser.getLocalName();
                        if ("properties".equals(element)) {
                            inProperties++;
                            writer.writeStartElement(element);
                        } else if (inProperties > 0) {
                            property = element;
                            if (!property.equals(oldProperty1) && !property.equals(oldProperty2)) {
                                writer.writeStartElement(element);
                            }
                        } else {
                            writer.writeStartElement(element);
                        }
                        break;
                    }

                    case XMLStreamConstants.END_ELEMENT: {
                        String element = parser.getLocalName();
                        if ("properties".equals(element)) {
                            writer.writeEndElement();
                            inProperties--;
                        } else if (inProperties > 0 && property != null) {
                            if (!property.equals(oldProperty1) && !property.equals(oldProperty2)) {
                                writer.writeEndElement();
                            }
                            property = null;
                        } else {
                            writer.writeEndElement();
                        }
                        break;
                    }

                    case XMLStreamConstants.CHARACTERS: {
                        String value = parser.getText();
                        if (property != null) {
                            if (!property.equals(oldProperty1) && !property.equals(oldProperty2)) {
                                writer.writeCharacters(value);
                            }
                        } else {
                            writer.writeCharacters(value);
                        }
                        break;
                    }
                } // end switch
            } // end for
            parser.close();

            writer.writeEndDocument();
            writer.flush();
            writer.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, "IO Error", ex);
            return;
        } catch (XMLStreamException ex) {
            log.log(Level.SEVERE, "Error when reading the XML document", ex);
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

        if (updatedPom.exists()) {
            pom.delete();
            if (!updatedPom.renameTo(pom)) {
                System.err.println("Cannot update " + pom.getAbsolutePath());
            }
        }

    }

    private boolean isDeclaration(String element) {
        return "groupId".equals(element) || "artifactId".equals(element) || "version".equals(element);
    }

    public static void main(String[] args) {
        try {
            DebianPOM dp = new DebianPOM();
            File pom = new File(args[1]);
            File updatedPom = File.createTempFile("pom", "tmp");

            if (args[0].equals("add")) {
                String property = args[2];
                String version = args[3];
                String pkg = args[4];

                Map properties = new HashMap();
                properties.put(property + ".version", version);
                properties.put(property + ".package", pkg);
                dp.addLibrary(pom, updatedPom, properties);

            } else if (args[0].equals("remove")) {
                String property = args[2];
                dp.removeLibrary(pom, updatedPom, property + ".version", property + ".package");
            } else {
                System.err.println("Invalid arguments. Syntax is");
                System.err.println("mvn-repo add <property> <version> <package>");
                System.err.println("mvn-repo remove <property>");
            }
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}
