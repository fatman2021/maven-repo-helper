/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.Map.Entry;
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
            Set rules = testPom.getPublishedRules(true);
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

    public void report() {

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
                    Dependency dependency = (Dependency) j.next();
                    if (dependency.getVersion() == null || dependency.getVersion().contains("$")) {
                        System.out.println("\t\t" + dependency);
                    }
                }
                for (Iterator j = pomInfo.getPlugins().iterator(); j.hasNext();) {
                    Dependency dependency = (Dependency) j.next();
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

        for (Iterator i = issues.iterator(); i.hasNext();) {
            String issue = (String) i.next();
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
        if (!pomsWithNumberOfIssues.isEmpty()) {
            System.out.println("Top 10 POM files with issues:");
            int count = 0;
            for (Iterator i = pomsWithNumberOfIssues.values().iterator(); i.hasNext() && count < 10;) {
                List orderedPoms = (List) i.next();
                for (Iterator j = orderedPoms.iterator(); j.hasNext() && count < 10; count++) {
                    File pom = (File) j.next();
                    List missingDeps = (List) pomsWithIssues.get(pom);
                    System.out.println("Missing dependencies in " + pom);
                    for (Iterator k = missingDeps.iterator(); k.hasNext();) {
                        Dependency dependency = (Dependency) k.next();
                        System.out.println("\t" + dependency);
                    }
                }
            }
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
            System.out.println("Top 10 missing dependencies:");
            int count = 0;
            for (Iterator i = missingDependenciesCountList.iterator(); i.hasNext() && count < 10; count++) {
                Map.Entry entry = (Entry) i.next();
                Dependency missingDependency = (Dependency) entry.getKey();
                Integer numberOfTimes = (Integer) entry.getValue();
                System.out.println("Missing dependency " + missingDependency + " is needed in " + numberOfTimes + " places");
            }
        }
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
        String repoLocation = "/usr/share/maven-repo";
        if (args.length > 0) {
            repoLocation = args[0];
        }
        System.out.println("Scanning repository...");
        Repository repository = new Repository(new File(repoLocation));
        repository.scan();
        repository.report();
        System.out.println("Done.");
    }
}
