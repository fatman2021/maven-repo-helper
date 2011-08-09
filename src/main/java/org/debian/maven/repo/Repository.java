/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.stream.XMLStreamException;

/**
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Repository {

    private static final Logger log = Logger.getLogger(Repository.class.getName());
    private File baseDir;
    private Map unresolvedPoms = new HashMap();
    private Map dep2info = new HashMap();
    private Map pomsWithMissingParent = new HashMap();
    private Map pomsWithMissingVersions = new HashMap();
    private Map resolvedPoms = new HashMap();
    private POMInfo superPom;
    private POMReader pomReader = new POMReader();
    private boolean scanned = false;

    public Repository(File baseDir) {
        this.baseDir = baseDir;
        try {
            InputStream superPomSource = getClass().getResourceAsStream("/org/apache/maven/project/pom-4.0.0.xml");
            // The maven2 jars may not always be present in the classpath
            if (superPomSource != null) {
                superPom = pomReader.readPom(new InputStreamReader(superPomSource));
                superPom.getThisPom().setGroupId("__super__");
                superPom.getThisPom().setArtifactId("__pom__");
                superPom.getThisPom().setType("pom");
            }
        } catch (XMLStreamException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public File getBaseDir() {
        return baseDir;
    }

    public POMInfo getSuperPOM() {
        return superPom;
    }

    public POMInfo getPOM(Dependency dependency) {
        return (POMInfo) dep2info.get(dependency);
    }

    protected Map getUnresolvedPoms() {
        return unresolvedPoms;
    }

    protected Map getPomsWithMissingParent() {
        return pomsWithMissingParent;
    }

    protected Map getPomsWithMissingVersions() {
        return pomsWithMissingVersions;
    }

    protected Map getResolvedPoms() {
        return resolvedPoms;
    }

    protected List getAllPoms() {
        List allPoms = new ArrayList(resolvedPoms.values());
        allPoms.addAll(unresolvedPoms.values());
        return allPoms;
    }

    /**
     * Search the best match for a dependency
     *
     * @param dependency
     * @return
     */
    public POMInfo searchMatchingPOM(Dependency dependency) {
        POMInfo pom = getPOM(dependency);
        if (pom != null) {
            return pom;
        }

        // Map<DependencyRule,POMInfo>
        Map potentialMatches = new TreeMap();
        for (Iterator i = getAllPoms().iterator(); i.hasNext();) {
            POMInfo testPom = (POMInfo) i.next();
            Set rules = testPom.getPublishedRules();
            rules.add(DependencyRule.MAVEN_PLUGINS_KEEP_VERSION_RULE);
            rules.add(DependencyRule.TO_DEBIAN_VERSION_RULE);
            
            for (Iterator j = rules.iterator(); j.hasNext();) {
                DependencyRule rule = (DependencyRule) j.next();
                if (rule.matches(dependency) && rule.apply(dependency).equals(testPom.getThisPom())) {
                    potentialMatches.put(rule, testPom);
                }
            }
        }
        if (!potentialMatches.isEmpty()) {
            // Return the best match
            return (POMInfo) potentialMatches.values().iterator().next();
        }
        return null;
    }

    public Iterator resolvedPomsIterator() {
        return resolvedPoms.values().iterator();
    }

    public List searchMatchingPOMsIgnoreVersion(Dependency dependency) {
        List result = new ArrayList();
        POMInfo pom = searchMatchingPOM(dependency);
        if (pom != null) {
            result.add(pom);
            return result;
        }

        for (Iterator i = resolvedPomsIterator(); i.hasNext();) {
            POMInfo testPom = (POMInfo) i.next();
            if (testPom.getThisPom().equalsIgnoreVersion(dependency)) {
                result.add(testPom);
            }
        }
        return result;
    }

    public void scanOnce() {
        if (scanned) {
            return;
        }
        scan();
    }

    public void scan() {
        File[] files = baseDir.listFiles();
        scan(files);
        resolveAll(unresolvedPoms);
        int unresolved = pomsWithMissingParent.size();
        while (unresolved > 0) {
            resolveAll(pomsWithMissingParent);
            if (pomsWithMissingParent.size() == unresolved) {
                // stale detection
                break;
            }
            unresolved = pomsWithMissingParent.size();
        }
        unresolved = unresolvedPoms.size();
        while (unresolved > 0) {
            resolveAll(unresolvedPoms);
            if (unresolvedPoms.size() == unresolved) {
                // stale detection
                break;
            }
            unresolved = unresolvedPoms.size();
        }
        scanned = true;
    }

    public void report(RepositoryReportWriter writer) {

    	writer.printStart();
    	
        if (pomsWithMissingParent.size() > 0) {
        	writer.printSectionStart("POMs with missing parents");
            for (Iterator i = pomsWithMissingParent.keySet().iterator(); i.hasNext();) {
                File pom = (File) i.next();
                writer.printItem(pom.getAbsolutePath());
                writer.endItem();
            }
            writer.printSectionEnd();
        }
        if (pomsWithMissingVersions.size() > 0) {
        	writer.printSectionStart("POMs with missing versions");
            for (Iterator i = pomsWithMissingVersions.entrySet().iterator(); i.hasNext();) {
                Entry entry = (Entry) i.next();
                File pom = (File) entry.getKey();
                POMInfo pomInfo = (POMInfo) entry.getValue();
                writer.printItem(pom.getAbsolutePath());
                for (Iterator j = pomInfo.getDependencies().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency) j.next();
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                    	writer.printItem(dependency.toString());
                    	writer.endItem();
                    }
                }
                for (Iterator j = pomInfo.getPlugins().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency) j.next();
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                    	writer.printItem(dependency.toString());
                    	writer.endItem();
                    }
                }
                writer.endItem();
            }
            writer.printSectionEnd();
        }

        Set issues = new TreeSet();
        Map pomsWithIssues = new HashMap();
        for (Iterator i = resolvedPoms.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            File pom = (File) entry.getKey();
            POMInfo pomInfo = (POMInfo) entry.getValue();
            if (pomInfo.getThisPom().getVersion() == null) {
                issues.add("Missing version in " + pom);
            }
            if (pomInfo.getThisPom().getVersion().endsWith("-SNAPSHOT")) {
                issues.add("Snapshot version in " + pom);
            }
            for (Iterator j = pomInfo.getDependencies().iterator(); j.hasNext();) {
                Dependency dependency = (Dependency) j.next();
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
                Dependency dependency = (Dependency) j.next();
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

        writer.printSectionStart("Errors");
        for (Iterator i = issues.iterator(); i.hasNext();) {
            String issue = (String) i.next();
            writer.printItem(issue);
            writer.endItem();
        }
        writer.printSectionEnd();

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
        if (!pomsWithNumberOfIssues.isEmpty()) {
        	writer.printSectionStart("Top 10 POM files with issues");
            int count = 0;
            for (Iterator i = pomsWithNumberOfIssues.values().iterator(); i.hasNext() && count < 10;) {
                List orderedPoms = (List) i.next();
                for (Iterator j = orderedPoms.iterator(); j.hasNext() && count < 10; count++) {
                    File pom = (File) j.next();
                    List missingDeps = (List) pomsWithIssues.get(pom);
                    writer.printItem("Missing dependencies in " + pom);
                    for (Iterator k = missingDeps.iterator(); k.hasNext();) {
                        Dependency dependency = (Dependency) k.next();
                        writer.printItem(dependency.toString());
                        writer.endItem();
                    }
                    writer.endItem();
                }
            }
            writer.printSectionEnd();
        }

        // Find the dependencies that need packaging most
        Map missingDependenciesCounts = new HashMap();
        for (Iterator i = pomsWithIssues.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            List missingDeps = (List) entry.getValue();
            for (Iterator j = missingDeps.iterator(); j.hasNext();) {
                Dependency missingDependency = (Dependency) j.next();
                Integer lastCount = (Integer) missingDependenciesCounts.remove(missingDependency);
                if (lastCount == null) {
                    lastCount = new Integer(0);
                }
                missingDependenciesCounts.put(missingDependency, new Integer(lastCount.intValue() + 1));
            }
        }
        List missingDependenciesCountList = new ArrayList(missingDependenciesCounts.entrySet());
        Collections.sort(missingDependenciesCountList, new Comparator() {

            public int compare(Object o, Object o2) {
                Map.Entry entry1 = (Entry) o;
                Map.Entry entry2 = (Entry) o2;
                Integer count1 = (Integer) entry1.getValue();
                Integer count2 = (Integer) entry2.getValue();
                return count2.compareTo(count1);
            }
        });
        if (! missingDependenciesCountList.isEmpty()) {
        	writer.printSectionStart("Top 10 missing dependencies");
            int count = 0;
            for (Iterator i = missingDependenciesCountList.iterator(); i.hasNext() && count < 10; count++) {
                Map.Entry entry = (Entry) i.next();
                Dependency missingDependency = (Dependency) entry.getKey();
                Integer numberOfTimes = (Integer) entry.getValue();
                writer.printItem("Missing dependency " + missingDependency + " is needed in " + numberOfTimes + " places");
                writer.endItem();
            }
            writer.printSectionEnd();
        }
        
        writer.printEnd();
    }

    private void resolveAll(Map file2pom) {
        // copy to avoid concurrent modifications
        Map copy = new HashMap(file2pom);
        for (Iterator i = copy.entrySet().iterator(); i.hasNext();) {
            Entry entry = (Entry) i.next();
            try {
                registerPom((File) entry.getKey(), (POMInfo) entry.getValue());
            } catch (DependencyNotFoundException e) {
                // Ignore
            }
        }
    }

    private void scan(File[] files) {
        if (files == null) {
            return;
        }
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (file.isDirectory()) {
                scan(file.listFiles());
            } else if (file.getName().endsWith(".pom")) {
                try {
                    POMInfo pom = pomReader.readPom(file);
                    registerPom(file, pom);
                } catch (XMLStreamException ex) {
                    ex.printStackTrace();
                } catch (FileNotFoundException ex) {
                    ex.printStackTrace();
                } catch (DependencyNotFoundException ex) {
                    // Ignore
                }
            }
        }
    }

    public void registerPom(File file, POMInfo pomInfo) throws DependencyNotFoundException {
        dep2info.put(pomInfo.getThisPom(), pomInfo);
        unresolvedPoms.put(file, pomInfo);

        POMInfo parentPOM = superPom;
        try {
            if (pomInfo.getParent() != null) {
                POMInfo foundParent = getPOM(pomInfo.getParent());
                if (foundParent == null) {
                    pomsWithMissingParent.put(file, pomInfo);
                    throw new DependencyNotFoundException(pomInfo.getParent());
                } else {
                    parentPOM = foundParent;
                    pomsWithMissingParent.remove(file);
                }
                if (!resolvedPoms.values().contains(parentPOM)) {
                    throw new DependencyNotFoundException(parentPOM.getThisPom());
                }
            }
        } finally {
            // Always merge with the parent POM - which is by default the super POM,
            // as we can have intermediate situations in the DependenciesSolver where
            // the true parent POM is not known and will be eliminated, yet we need
            // the versions from the super POM.
            pomInfo.mergeManagement(parentPOM);

            pomsWithMissingVersions.remove(file);
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

        resolvedPoms.put(file, pomInfo);
        unresolvedPoms.remove(file);
    }

    public static void main(String[] args) {
        File repoLocation = new File("/usr/share/maven-repo");

        if (args != null && "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: Check content of a Maven repository.");
            System.out.println("Usage: [option]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -o<format>, --output=<format>: 'text' or 'html' to change output format");
            System.out.println("  -r<repository>, --repository=<repository>: directory of Maven repository");
            return;
        }

        String format = "text";

        // Parse parameters
        int i = inc(-1, args);
        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().isEmpty())) {
            String arg = args[i].trim();
            if (arg.startsWith("-o")) {
                format = arg.substring(2);
            } else if (arg.startsWith("--output=")) {
                format = arg.substring("--output=".length());
             } else if (arg.startsWith("-r")) {
                repoLocation = new File(arg.substring(2));
            } else if (arg.startsWith("--repository=")) {
            	repoLocation = new File(arg.substring("--repository=".length()));
            }
            i = inc(i, args);
        }

        Repository repository = new Repository(repoLocation);
        RepositoryReportWriter reportWriter;
        if ("text".equals(format)) {
            reportWriter = new RepositoryReportTextWriter();
        } else {
            reportWriter = new RepositoryReportHTMLWriter();
        }
        
        repository.scan();
        repository.report(reportWriter);
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].isEmpty());
        return i;
    }

}
