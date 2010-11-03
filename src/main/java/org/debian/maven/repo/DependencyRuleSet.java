package org.debian.maven.repo;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class DependencyRuleSet {

    private static final Logger log = Logger.getLogger(DependencyRuleSet.class.getName());

    private File rulesFile;
    private Set rules;
    private boolean verbose;
    private boolean warnRulesFileNotFound = true;
    private String name;
    private String description;

    public DependencyRuleSet(String name) {
        this.name = name;
    }

    public DependencyRuleSet(String name, File rulesFile) {
        this(name);
        this.rulesFile = rulesFile;
    }

    public File getRulesFile() {
        return rulesFile;
    }

    public void setRulesFile(File rulesFile) {
        this.rulesFile = rulesFile;
        rules = null;
        readRules();
    }

    public Set getRules() {
        if (rules == null) {
            readRules();
        }
        return rules;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public boolean isWarnRulesFileNotFound() {
        return warnRulesFileNotFound;
    }

    public void setWarnRulesFileNotFound(boolean warnRulesFileNotFound) {
        this.warnRulesFileNotFound = warnRulesFileNotFound;
    }

    public Iterator iterator() {
        return getRules().iterator();
    }

    public boolean isEmpty() {
        return getRules().isEmpty();
    }

    public void add(DependencyRule rule) {
        getRules().add(rule);
    }

    public void addAll(DependencyRuleSet newRules) {
        for (Iterator i = newRules.iterator(); i.hasNext();) {
            DependencyRule rule = (DependencyRule) i.next();
            add(rule);
        }
    }

    public void addAll(Collection newRules) {
        for (Iterator i = newRules.iterator(); i.hasNext();) {
            Object rule = i.next();
            if (rule instanceof DependencyRule) {
                add((DependencyRule) rule);
            } else {
                add(new DependencyRule((String) rule));
            }
        }
    }

    public Set findMatchingRules(Dependency dependency) {
        Set matchingRules = new HashSet();
        for (Iterator i = rules.iterator(); i.hasNext();) {
            DependencyRule rule = (DependencyRule) i.next();
            if (rule.matches(dependency)) {
                matchingRules.add(rule);
            }
        }
        return matchingRules;
    }

    public void save() {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(getRulesFile()));
            out.println(description);

            for (Iterator i = getRules().iterator(); i.hasNext();) {
                DependencyRule rule = (DependencyRule) i.next();
                out.println(rule.toString());
            }
            out.flush();
            out.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void readRules() {
        rules = new TreeSet();
        if (rulesFile == null) {
            return;
        }
        if (!rulesFile.exists()) {
            if (verbose && warnRulesFileNotFound) {
                String descr = description;
                if (description == null || description.isEmpty()) {
                    descr = "Rules";
                }
                System.out.println(descr + " file does not exist: " + rulesFile.getAbsolutePath());
            }
            return;
        }
        try {
            if (verbose) {
                System.out.println(name + ":");
            }
            LineNumberReader lnr = new LineNumberReader(new FileReader(rulesFile));
            String line;
            while ((line = lnr.readLine()) != null) {
                line = line.trim();
                if (line.length() > 0 && !line.startsWith("#")) {
                    if (verbose) {
                        System.out.println("  " + line);
                    }
                    rules.add(new DependencyRule(line));
                }
            }
            if (verbose) {
                System.out.println("---------");
            }

        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

}
