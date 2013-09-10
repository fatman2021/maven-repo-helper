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

package org.debian.maven.repo;

import java.io.File;
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

import org.debian.maven.repo.POMInfo.DependencyType;

import static org.debian.maven.repo.DependencyRuleSet.*;

/**
 * View of a Maven repository.
 * 
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class Repository {

    private static final Logger log = Logger.getLogger(Repository.class.getName());

    /** The base directory of the repository typically (/usr/share/maven-repo) */
    private File baseDir;

    private Map<File, POMInfo> unresolvedPoms = new HashMap<File, POMInfo>();

    /** The dependency (groupId+artifactId+version+type) to pom mapping */
    private Map<Dependency, POMInfo> dep2info = new HashMap<Dependency, POMInfo>();

    /** The pom files specifying a parent pom not found in the repository */
    private Map<File, POMInfo> pomsWithMissingParent = new HashMap<File, POMInfo>();

    /** The pom files specifying a plugin or a dependency without version */
    private Map<File, POMInfo> pomsWithMissingVersions = new HashMap<File, POMInfo>();
    
    /** The resolved poms, that is the poms with a resolved parent */
    private Map<File, POMInfo> resolvedPoms = new HashMap<File, POMInfo>();

    /** The Maven super pom defining the default plugins */
    private POMInfo superPom;

    /** The parser used to read the pom files */
    private POMReader pomReader = new POMReader();
    
    /** Tells if the directory of the repository has already been scanned */
    private boolean scanned = false;

    public Repository(File baseDir) {
        this.baseDir = baseDir;
        
        // load the Maven super pom
        try {
            InputStream superPomSource = getClass().getResourceAsStream("/org/apache/maven/project/pom-4.0.0.xml");
            // The maven2 jars may not always be present in the classpath
            if (superPomSource != null) {
                superPom = pomReader.readPom(new InputStreamReader(superPomSource));
                superPom.getThisPom().setGroupId("__super__");
                superPom.getThisPom().setArtifactId("__pom__");
                superPom.getThisPom().setType("pom");
                superPom.getThisPom().setSuperPom(true);
            }
        } catch (XMLStreamException e) {
            log.log(Level.SEVERE, "Unable to load the Maven super pom", e);
        }
    }

    /**
     * Returns the base directory of the Maven repository.
     */
    public File getBaseDir() {
        return baseDir;
    }

    /**
     * Returns the Maven super pom defining the default plugins.
     */
    public POMInfo getSuperPOM() {
        return superPom;
    }

    /**
     * Returns the pom matching exactly the specified dependency.
     * 
     * @param dependency
     */
    public POMInfo getPOM(Dependency dependency) {
        return dep2info.get(dependency);
    }

    protected Map<File, POMInfo> getUnresolvedPoms() {
        return unresolvedPoms;
    }

    protected Map<File, POMInfo> getPomsWithMissingParent() {
        return pomsWithMissingParent;
    }

    protected Map<File, POMInfo> getPomsWithMissingVersions() {
        return pomsWithMissingVersions;
    }

    protected Map<File, POMInfo> getResolvedPoms() {
        return resolvedPoms;
    }

    protected List<POMInfo> getAllPoms() {
        List<POMInfo> allPoms = new ArrayList<POMInfo>(resolvedPoms.values());
        allPoms.addAll(unresolvedPoms.values());
        return allPoms;
    }

    /**
     * Search the best match for a dependency.
     *
     * @param dependency
     */
    public POMInfo searchMatchingPOM(Dependency dependency) {
        if (dependency == null) {
            return null;
        }
        
        POMInfo pom = getPOM(dependency);
        if (pom != null) {
            return pom;
        }

        Map<DependencyRule, POMInfo> potentialMatches = new TreeMap<DependencyRule, POMInfo>();
        for (POMInfo testPom : getAllPoms()) {
            Set<DependencyRule> rules = testPom.getPublishedRules();
            rules.add(MAVEN_PLUGINS_KEEP_VERSION_RULE);
            rules.add(TO_DEBIAN_VERSION_RULE);
            
            for (DependencyRule rule : rules) {
                if (rule.matches(dependency) && rule.apply(dependency).equals(testPom.getThisPom())) {
                    potentialMatches.put(rule, testPom);
                }
            }
        }
        if (!potentialMatches.isEmpty()) {
            // Return the best match
            return potentialMatches.values().iterator().next();
        }
        return null;
    }

    public List<POMInfo> searchMatchingPOMsIgnoreVersion(Dependency dependency) {
        List<POMInfo> result = new ArrayList<POMInfo>();
        POMInfo pom = searchMatchingPOM(dependency);
        if (pom != null) {
            result.add(pom);
            return result;
        }

        for (POMInfo testPom : resolvedPoms.values()) {
            if (testPom.getThisPom().equalsIgnoreVersion(dependency)) {
                result.add(testPom);
            }
        }
        return result;
    }

    public void scanOnce() {
        if (!scanned) {
            scan();
        }
    }

    public void scan() {
        scan(baseDir.listFiles());
         
        resolveAll(unresolvedPoms);
        
        // resolve the missing parents
        int unresolved = pomsWithMissingParent.size();
        while (unresolved > 0) {
            resolveAll(pomsWithMissingParent);
            if (pomsWithMissingParent.size() == unresolved) {
                // stale detection
                break;
            }
            unresolved = pomsWithMissingParent.size();
        }
        
        // update the resolved poms once all the available parents are known
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
            for (File pom : pomsWithMissingParent.keySet()) {
                writer.printItem(pom.getAbsolutePath());
                writer.endItem();
            }
            writer.printSectionEnd();
        }
        if (pomsWithMissingVersions.size() > 0) {
            writer.printSectionStart("POMs with missing versions");
            for (Entry<File, POMInfo> entry : pomsWithMissingVersions.entrySet()) {
                File pom = entry.getKey();
                POMInfo pomInfo = entry.getValue();
                writer.printItem(pom.getAbsolutePath());
                for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.DEPENDENCIES)) {
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                        writer.printItem(dependency.toString());
                        writer.endItem();
                    }
                }
                for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.PLUGINS)) {
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                        writer.printItem(dependency.toString());
                        writer.endItem();
                    }
                }
                writer.endItem();
            }
            writer.printSectionEnd();
        }

        Set<String> issues = new TreeSet<String>();
        Map<File, List<Dependency>> pomsWithIssues = new HashMap<File, List<Dependency>>();
        for (Entry<File, POMInfo> entry : resolvedPoms.entrySet()) {
            File pom = entry.getKey();
            POMInfo pomInfo = entry.getValue();
            if (pomInfo.getThisPom().getVersion() == null) {
                issues.add("Missing version in " + pom);
            }
            if (pomInfo.getThisPom().getVersion().endsWith("-SNAPSHOT")) {
                issues.add("Snapshot version in " + pom);
            }
            for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.DEPENDENCIES)) {
                if (!dep2info.containsKey(dependency)) {
                    issues.add("Unpackaged dependency: " + dependency + " in " + pom);
                    List<Dependency> pomIssues = pomsWithIssues.get(pom);
                    if (pomIssues == null) {
                        pomIssues = new ArrayList<Dependency>();
                        pomsWithIssues.put(pom, pomIssues);
                    }
                    pomIssues.add(dependency);
                }
            }
            for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.PLUGINS)) {
                if (!dep2info.containsKey(dependency)) {
                    issues.add("Unpackaged plugin: " + dependency + " in " + pom);
                    List<Dependency> pomIssues = pomsWithIssues.get(pom);
                    if (pomIssues == null) {
                        pomIssues = new ArrayList<Dependency>();
                        pomsWithIssues.put(pom, pomIssues);
                    }
                    pomIssues.add(dependency);
                }
            }
        }

        writer.printSectionStart("Errors");
        for (String issue : issues) {
            writer.printItem(issue);
            writer.endItem();
        }
        writer.printSectionEnd();

        // Find the poms with most issues
        Map<Integer, List<File>> pomsWithNumberOfIssues = new TreeMap<Integer, List<File>>(Collections.reverseOrder());
        for (Entry<File, List<Dependency>> entry : pomsWithIssues.entrySet()) {
            File pom = entry.getKey();
            List<Dependency> missingDeps = entry.getValue();
            int count = missingDeps.size();
            List<File> orderedPoms = pomsWithNumberOfIssues.get(count);
            if (orderedPoms == null) {
                orderedPoms = new ArrayList<File>();
                pomsWithNumberOfIssues.put(count, orderedPoms);
            }
            orderedPoms.add(pom);
        }
        if (!pomsWithNumberOfIssues.isEmpty()) {
            writer.printSectionStart("Top 10 POM files with issues");
            int count = 0;
            for (Iterator<List<File>> i = pomsWithNumberOfIssues.values().iterator(); i.hasNext() && count < 10; ) {
                List<File> orderedPoms = i.next();
                for (Iterator<File> j = orderedPoms.iterator(); j.hasNext() && count < 10; count++) {
                    File pom = j.next();
                    List<Dependency> missingDeps = pomsWithIssues.get(pom);
                    writer.printItem("Missing dependencies in " + pom);
                    for (Dependency dependency : missingDeps) {
                        writer.printItem(dependency.toString());
                        writer.endItem();
                    }
                    writer.endItem();
                }
            }
            writer.printSectionEnd();
        }

        // Find the dependencies that need packaging most
        Map<Dependency, Integer> missingDependenciesCounts = new HashMap<Dependency, Integer>();
        for (Entry<File, List<Dependency>> entry : pomsWithIssues.entrySet()) {
            List<Dependency> missingDeps = entry.getValue();
            for (Dependency missingDependency : missingDeps) {
                Integer lastCount = missingDependenciesCounts.remove(missingDependency);
                if (lastCount == null) {
                    lastCount = 0;
                }
                missingDependenciesCounts.put(missingDependency, lastCount + 1);
            }
        }
        List<Map.Entry<Dependency, Integer>> missingDependenciesCountList = new ArrayList<Map.Entry<Dependency, Integer>>(missingDependenciesCounts.entrySet());
        Collections.sort(missingDependenciesCountList, new Comparator<Map.Entry<Dependency, Integer>>() {

            public int compare(Map.Entry<Dependency, Integer> entry1, Map.Entry<Dependency, Integer> entry2) {
                Integer count1 = entry1.getValue();
                Integer count2 = entry2.getValue();
                return count2.compareTo(count1);
            }
        });
        if (!missingDependenciesCountList.isEmpty()) {
            writer.printSectionStart("Top 10 missing dependencies");
            int count = 0;
            for (Iterator<Map.Entry<Dependency, Integer>> i = missingDependenciesCountList.iterator(); i.hasNext() && count < 10; count++) {
                Map.Entry<Dependency, Integer> entry = i.next();
                Dependency missingDependency = entry.getKey();
                Integer numberOfTimes = entry.getValue();
                writer.printItem("Missing dependency " + missingDependency + " is needed in " + numberOfTimes + " places");
                writer.endItem();
            }
            writer.printSectionEnd();
        }

        writer.printEnd();
    }

    private void resolveAll(Map<File, POMInfo> file2pom) {
        // copy to avoid concurrent modifications
        Map<File, POMInfo> copy = new HashMap<File, POMInfo>(file2pom);
        for (Entry<File, POMInfo> entry : copy.entrySet()) {
            try {
                registerPom(entry.getKey(), entry.getValue());
            } catch (DependencyNotFoundException e) {
                // Ignore
            }
        }
    }

    private void scan(File[] files) {
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                scan(file.listFiles());
            } else if (file.getName().endsWith(".pom")) {
                try {
                    registerPom(file, pomReader.readPom(file));
                } catch (DependencyNotFoundException e) {
                    // Ignore
                } catch (Exception e) {
                    System.out.println("Failed to parse " + file);
                    e.printStackTrace();
                    System.out.println();
                }
            }
        }
    }

    public void registerPom(File file, POMInfo pomInfo) throws DependencyNotFoundException {
        dep2info.put(pomInfo.getThisPom(), pomInfo);
        unresolvedPoms.put(file, pomInfo);

        POMInfo parentPOM = superPom;
        try {
            if (pomInfo.getParent() != null && !pomInfo.getParent().isSuperPom()) {
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
            pomInfo.setParentPOM(parentPOM);

            // check if the pom specifies plugins or dependencies without an explicit version
            pomsWithMissingVersions.remove(file);
            for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.DEPENDENCIES)) {
                if (dependency.getVersion() == null) {
                    pomsWithMissingVersions.put(file, pomInfo);
                }
            }
            for (Dependency dependency : pomInfo.getDependencies().get(DependencyType.PLUGINS)) {
                if (dependency.getVersion() == null) {
                    pomsWithMissingVersions.put(file, pomInfo);
                }
            }
        }

        // mark the pom as resolved
        resolvedPoms.put(file, pomInfo);
        unresolvedPoms.remove(file);
    }

    public static void main(String[] args) {
        if (args != null && args.length > 0 && ("-h".equals(args[0]) || "--help".equals(args[0]))) {
            System.out.println("Purpose: Check content of a Maven repository.");
            System.out.println("Usage: [option]");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -o<format>, --output=<format>: 'text' or 'html' to change output format");
            System.out.println("  -r<repository>, --repository=<repository>: directory of Maven repository");
            return;
        }

        // default format
        String format = "text";
        // default repository
        File repoLocation = new File("/usr/share/maven-repo/");

        // Parse parameters
        int i = inc(-1, args);
        if (args != null) {
            while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().isEmpty())) {
                String arg = args[i].trim();
                if (arg.startsWith("-o")) {
                    format = arg.substring(2).trim();
                } else if (arg.startsWith("--output=")) {
                    format = arg.substring("--output=".length()).trim();
                } else if (arg.startsWith("-r")) {
                    repoLocation = new File(arg.substring(2).trim());
                } else if (arg.startsWith("--repository=")) {
                    repoLocation = new File(arg.substring("--repository=".length()).trim());
                }
                i = inc(i, args);
            }
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
