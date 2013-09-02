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

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class DependencyRuleSet implements Iterable<DependencyRule> {

    static final DependencyRule TO_DEBIAN_VERSION_RULE = new DependencyRule("");
    static final DependencyRule MAVEN_PLUGINS_KEEP_VERSION_RULE = new DependencyRule("* * maven-plugin * * *");
    static final DependencyRule NO_CHANGE_RULE = new DependencyRule("* * * * * *");

    private static final Logger log = Logger.getLogger(DependencyRuleSet.class.getName());

    private final Set<DependencyRule> rules;
    private final String description;

    public Set<DependencyRule> getRules() {
        return rules;
    }

    public DependencyRuleSet(Set<DependencyRule> rules, String description) {
        this.rules = rules;
        this.description = description;
    }

    public DependencyRuleSet(String description) {
        this(new TreeSet<DependencyRule>(), description);
    }

    public DependencyRuleSet() {
        this("");
    }

    public Iterator<DependencyRule> iterator() {
        return getRules().iterator();
    }

    public boolean isEmpty() {
        return getRules().isEmpty();
    }

    public void add(DependencyRule rule) {
        getRules().add(rule);
    }

    public void addAll(DependencyRuleSet newRules) {
        for (DependencyRule rule: newRules) {
            add(rule);
        }
    }

    public void addAll(Collection<?> newRules) {
        for (Object rule : newRules) {
            if (rule instanceof DependencyRule) {
                add((DependencyRule) rule);
            } else {
                add(new DependencyRule((String) rule));
            }
        }
    }

    @Deprecated
    public void remove(DependencyRule rule) {
        getRules().remove(rule);
    }

    public Set<DependencyRule> findMatchingRules(Dependency dependency) {
        Set<DependencyRule> matchingRules = new HashSet<DependencyRule>();
        for (DependencyRule rule : rules) {
            if (rule.matches(dependency)) {
                matchingRules.add(rule);
            }
        }
        return matchingRules;
    }

    public void saveToFile(File rulesFile, DependencyRuleSet dontDuplicate) {
        try {
            PrintWriter out = new PrintWriter(new FileWriter(rulesFile));
            out.println(description);

            for (DependencyRule rule: getRules()) {
                if ((dontDuplicate == null || !dontDuplicate.getRules().contains(rule))
                        // Don't save implicit rules
                        && !TO_DEBIAN_VERSION_RULE.equals(rule)
                        && !MAVEN_PLUGINS_KEEP_VERSION_RULE.equals(rule)) {
                    out.println(rule.toString());
                }
            }
            out.flush();
            out.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public void dump() {
        if (rules != null) {
            // System.out.println(name + ":");
            for (DependencyRule rule : rules) {
                System.out.println("  " + rule);
            }
            System.out.println("---------");
        }
    }

    public static DependencyRuleSet readRules(File rulesFile, String description, boolean verbose, boolean warnRulesFileNotFound) {
        DependencyRuleSet rules = new DependencyRuleSet(description);
        if (rulesFile == null) {
            return rules;
        }
        if (!rulesFile.exists()) {
            if (verbose && warnRulesFileNotFound) {
                String descr = description;
                if (description == null || description.isEmpty()) {
                    descr = "Rules";
                }
                System.out.println(descr + " file does not exist: " + rulesFile.getAbsolutePath());
            }
            return rules;
        }
        try {
            if (verbose) {
                System.out.println(rulesFile.getName() + ":");
            }
            BufferedReader lnr = new BufferedReader(new FileReader(rulesFile));
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
            lnr.close();
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
        return rules;
    }
}
