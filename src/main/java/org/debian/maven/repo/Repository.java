/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import javax.xml.stream.XMLStreamException;

/**
 *
 * @author ludo
 */
public class Repository {

    private File baseDir;
    private Map poms = new HashMap();
    private Map dep2info = new HashMap();
    private Map pomsWithMissingParent = new HashMap();
    private Map pomsWithMissingVersions = new HashMap();
    private Map resolvedPoms = new HashMap();

    public Repository(File baseDir) {
        this.baseDir = baseDir;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void scan() {
        File[] files = baseDir.listFiles();
        scan(files);
        resolveAll(poms);
        int unresolved = pomsWithMissingParent.size();
        while (unresolved > 0) {
            resolveAll(pomsWithMissingParent);
            if (pomsWithMissingParent.size() == unresolved) {
                // stale detection
                break;
            }
            unresolved = pomsWithMissingParent.size();
        }
        unresolved = poms.size();
        while (unresolved > 0) {
            resolveAll(poms);
            if (poms.size() == unresolved) {
                // stale detection
                break;
            }
            unresolved = poms.size();
        }

        if (pomsWithMissingParent.size() > 0) {
            System.out.println("POMs with missing parents:");
            for (Iterator i = pomsWithMissingParent.keySet().iterator(); i.hasNext();) {
                File pom = (File) i.next();
                System.out.println("\t" + pom.getAbsolutePath());
            }
        }
        if (pomsWithMissingVersions.size() > 0) {
            System.out.println("POMs with missing versions:");
            for (Iterator i = pomsWithMissingVersions.entrySet().iterator(); i.hasNext();) {
                Entry entry = (Entry) i.next();
                File pom = (File) entry.getKey();
                POMInfo pomInfo = (POMInfo) entry.getValue();
                System.out.println("\t" + pom.getAbsolutePath());
                for (Iterator j = pomInfo.getDependencies().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency)j.next();
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                        System.out.println("\t\t" + dependency);
                    }
                }
                for (Iterator j = pomInfo.getPlugins().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency)j.next();
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                        System.out.println("\t\t" + dependency);
                    }
                }
            }
        }
        System.out.println();

        Set issues = new TreeSet();
        Map pomsWithIssues = new HashMap();
        for (Iterator i = resolvedPoms.entrySet().iterator(); i.hasNext();) {
                Entry entry = (Entry) i.next();
                File pom = (File) entry.getKey();
                POMInfo pomInfo = (POMInfo) entry.getValue();
                for (Iterator j = pomInfo.getDependencies().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency)j.next();
                    if (!dep2info.containsKey(dependency)) {
                        issues.add("Unpackaged dependency: " + dependency + " in " + pom);
                        List pomIssues = (List) pomsWithIssues.get(pom);
                        if (pomIssues == null) {
                            pomIssues = new ArrayList();
                            pomsWithIssues.put(pom, pomIssues);
                        }
                        pomIssues.add(dependency);
                    }
                }
                for (Iterator j = pomInfo.getPlugins().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency)j.next();
                    if (!dep2info.containsKey(dependency)) {
                        issues.add("Unpackaged plugin: " + dependency + " in " + pom);
                        List pomIssues = (List) pomsWithIssues.get(pom);
                        if (pomIssues == null) {
                            pomIssues = new ArrayList();
                            pomsWithIssues.put(pom, pomIssues);
                        }
                        pomIssues.add(dependency);
                    }
                }
        }

        for (Iterator i = issues.iterator(); i.hasNext();) {
            String issue = (String)i.next();
            System.out.println(issue);
        }
        System.out.println();

        // Find the poms with most issues
        Map pomsWithNumberOfIssues = new TreeMap(Collections.reverseOrder());
        for (Iterator i = pomsWithIssues.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            File pom = (File) entry.getKey();
            List missingDeps = (List) entry.getValue();
            int count = missingDeps.size();
            List orderedPoms = (List) pomsWithNumberOfIssues.get(new Integer(count));
            if (orderedPoms == null) {
                orderedPoms = new ArrayList();
                pomsWithNumberOfIssues.put(new Integer(count), orderedPoms);
            }
            orderedPoms.add(pom);
        }
        System.out.println("POM files with the most issues:");
        int count = 0;
        for (Iterator i = pomsWithNumberOfIssues.values().iterator(); i.hasNext() && count < 10; ) {
            List orderedPoms = (List) i.next();
            for (Iterator j = orderedPoms.iterator(); j.hasNext() && count < 10; count++) {
                File pom = (File) j.next();
                List missingDeps = (List) pomsWithIssues.get(pom);
                System.out.println("Missing dependencies in " + pom);
                for (Iterator k = missingDeps.iterator(); k.hasNext();) {
                    Dependency dependency = (Dependency)k.next();
                    System.out.println("\t" + dependency);
                }
            }
        }

    }

    private void resolveAll(Map file2pom) {
        // copy to avoid concurrent modifications
        Map copy = new HashMap(file2pom);
        for (Iterator i = copy.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            resolveDependencies((File) entry.getKey(), (POMInfo) entry.getValue());
        }
    }

    private void scan(File[] files) {
        POMReader pomReader = new POMReader();
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                scan(file.listFiles());
            } else if (file.getName().endsWith(".pom")) {
                try {
                    POMInfo pom = pomReader.readPom(file);
                    poms.put(file, pom);
                } catch (XMLStreamException ex) {
                    ex.printStackTrace();
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private void resolveDependencies(File file, POMInfo pomInfo) {
        dep2info.put(pomInfo.getThisPom(), pomInfo);
        POMInfo parentPOM = null;
        if (pomInfo.getParent() != null) {
            parentPOM = (POMInfo) dep2info.get(pomInfo.getParent());
            if (parentPOM == null) {
                pomsWithMissingParent.put(file, pomInfo);
                return;
            } else {
                pomsWithMissingParent.remove(file);
            }
            if (!resolvedPoms.values().contains(parentPOM)) {
                return;
            }
        }

        resolvedPoms.put(file, pomInfo);
        poms.remove(file);

        pomInfo.mergeManagement(parentPOM);

        for (Iterator i = pomInfo.getDependencies().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.getVersion() == null) {
                pomsWithMissingVersions.put(file, pomInfo);
            }
        }
        for (Iterator i = pomInfo.getPlugins().iterator(); i.hasNext();) {
            Dependency dependency = (Dependency) i.next();
            if (dependency.getVersion() == null) {
                pomsWithMissingVersions.put(file, pomInfo);
            }
        }
    }

    public static void main(String[] args) {
        String repoLocation = "/usr/share/maven-repo";
        if (args.length > 0) {
            repoLocation = args[0];
        }
        System.out.println("Scanning repository...");
        new Repository(new File(repoLocation)).scan();
        System.out.println("Done.");
    }
}
