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
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.stream.XMLStreamException;

import org.debian.maven.repo.POMInfo.DependencyType;
import static org.debian.maven.repo.DependencyRuleSetFiles.RulesType.*;

/**
 * Cleans up a POM for inclusion in the /usr/share/maven-repo/ repository.
 *
 * All POMs should contain only 
 *
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class POMCleaner extends POMTransformer {

    private static final Logger log = Logger.getLogger(POMCleaner.class.getName());
    private static final List<String> WRITE_IGNORED_ELEMENTS = Arrays.asList("build",
                "distributionManagement", "profiles", "ciManagement", "prerequisites",
                "repositories", "pluginRepositories", "reports", "reporting", "modelVersion",
                "parent");
    private boolean keepAllElements = false;
    private Collection<String> keepElements = new ArrayList<String>();

    public POMCleaner() {
    }

    public boolean isKeepAllElements() {
        return keepAllElements;
    }

    public void setKeepAllElements(boolean keepAllElements) {
        this.keepAllElements = keepAllElements;
    }

    public void addElementToKeep(String element) {
        keepElements.add(element);
    }

    public void cleanPom(File originalPom, File targetPom, File pomProperties,
            boolean noParent, boolean hasPackageVersion, boolean keepPomVersion,
            boolean keepParentVersion, String setVersion, String debianPackage) {

        if (targetPom.getParentFile() != null) {
            targetPom.getParentFile().mkdirs();
        }
        if (pomProperties.getParentFile() != null) {
            pomProperties.getParentFile().mkdirs();
        }

        try {
            POMInfo info = transformPom(originalPom, targetPom, noParent, hasPackageVersion, keepPomVersion, keepParentVersion, setVersion, debianPackage);

            Properties pomProps = new Properties();
            pomProps.put("groupId", info.getThisPom().getGroupId());
            pomProps.put("artifactId", info.getThisPom().getArtifactId());
            pomProps.put("type", info.getThisPom().getType());
            pomProps.put("version", info.getOriginalVersion());
            pomProps.put("debianVersion", info.getThisPom().getVersion());
            pomProps.put("classifier", info.getThisPom().getClassifier());
            FileOutputStream pomWriter = new FileOutputStream(pomProperties);
            pomProps.store(pomWriter, "POM properties");
            pomWriter.close();

        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        } catch (XMLStreamException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    protected void transformingPom(POMInfo pom) {
        if (pom.getThisPom().getType().equals("maven-plugin")) {
            getRulesFiles().get(IGNORE).add(new DependencyRule(pom.getThisPom().getGroupId() + " "
                    + pom.getThisPom().getArtifactId() + " maven-plugin s/.*/"
                    + pom.getThisPom().getVersion() + "/"));
        }
    }

    @Override
    protected boolean shouldWriteRelativePath() { return false; }

    @Override
    protected boolean isWriteIgnoredElement(String element, TreePath<String> path, Dependency dependency) {
        if (path.matches("parent/relativePath")) {
            return true;
        }
        boolean ignore = super.isWriteIgnoredElement(element, path, dependency);
        if (keepAllElements || ignore) {
            return ignore;
        }
        if (keepElements.contains(element)) {
            return false;
        }
        if (path.size() > 2 && keepElements.contains(path.get(1))) {
            if ("version".equals(element)) {
                if ("plugin".equals(path.parent(1))) {
                    return true;
                }
            }
            
        }
//        if (WRITE_IGNORED_ELEMENTS.contains(element)) {
//            System.out.println("Write ignored " + element + " " + printPath(path) + " for " + dependency);
//        }
        return path.size() == 2 && WRITE_IGNORED_ELEMENTS.contains(element);
    }

    protected boolean acceptDependency(Dependency dependency, POMInfo info) {
        return super.acceptDependency(dependency, info) &&
                ("pom".equals(info.getThisPom().getType()) || !"test".equals(dependency.getScope()));
    }

    protected void createDebianProperties(POMInfo info, POMInfo original, String debianPackage, int inLevel) throws XMLStreamException {
        super.createDebianProperties(info, original, debianPackage, inLevel);
        for (Dependency dependency : original.getDependencies().get(DependencyType.DEPENDENCIES)) {
            if (dependency.getVersion() != null) {
                String versionProperty = "debian." + dependency.getGroupId() + "." + dependency.getArtifactId() + ".originalVersion";
                info.getProperties().put(versionProperty, dependency.getVersion());
            }
        }
    }

    public static void main(String[] args) {
        if (args.length == 0 || "-h".equals(args[0]) || "--help".equals(args[0])) {
            System.out.println("Purpose: cleans a Maven POM for inclusion in the Debian/Maven repository in /usr/share/maven-repo");
            System.out.println("Usage: [option] original-pom target-file pom-properties");
            System.out.println("");
            System.out.println("Options:");
            System.out.println("  -v, --verbose: be extra verbose");
            System.out.println("  -o, --no-parent: don't inherit from a parent POM");
            System.out.println("  -p<package>, --package=<package>: name of the Debian package containing");
            System.out.println("    this library");
            System.out.println("  -h --has-package-version: flag that indicates that this POM has the");
            System.out.println("    same version as the package, this helps packagers of depending packages");
            System.out.println("  -r<rules>, --rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to apply when cleaning the POM");
            System.out.println("  -R<rule>, --extra-rule=<rule>: extra rule to apply when cleaning the POM");
            System.out.println("    May occur multiple times, instead of or in addition to -r");
            System.out.println("  -u<rules>, --published-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules to publish in the property debian.mavenRules in the cleaned POM");
            System.out.println("  -U<rule>, --extra-published-rule=<rule>: extra rule to publish");
            System.out.println("    May occur multiple times, instead of or in addition to -u");
            System.out.println("  -i<rules>, --ignore-rules=<rules>: path to the file containing the");
            System.out.println("    extra rules use to remove certain dependencies from the transformed POM");
            System.out.println("    This option can be repeated, in order to have multiple sets of");
            System.out.println("    dependencies to ignore, useful in situations such as when the Maven clean");
            System.out.println("    target requires more dependencies or plugins to ignore than the build target");
            System.out.println("  -I<rule>, --extra-ignore-rule=<rule>: extra rule used to remove dependencies");
            System.out.println("    from the transformed POM");
            System.out.println("    May occur multiple times, instead of or in addition to -i");
            System.out.println("  --no-rules: don't apply any rules for converting versions, ");
            System.out.println("    do not even convert versions to the default 'debian' version");
            System.out.println("  --no-publish-used-rule: don't publish the rule used to transform");
            System.out.println("    a POM's own attributes in debian.mavenRules");
            System.out.println("  -e<version>, --set-version=<version>: set the version for the POM,");
            System.out.println("    do not use the version declared in the POM file.");
            System.out.println("  --keep-pom-version: keep the original version of the POM but");
            System.out.println("    convert all other versions in dependencies and plugins");
            System.out.println("  --keep-parent-version: keep the version of the parent in the POM but");
            System.out.println("    convert all other versions in dependencies and plugins");
            System.out.println("  --keep-all-elements: keep all elements in the POM, do a version");
            System.out.println("    transformation only, don't delete the build and other elements.");
            System.out.println("  --keep-elements=<elem1,elem2>: keep the elements listed here");
            System.out.println("    even if they are normally removed by the clean operation.");
            System.out.println("    Such elements are build,reports,reporting,prerequisites,profiles.");
            System.out.println("  -m<repo root>--maven-repo=<repo root>: location of the Maven repository,");
            System.out.println("    used to force the versions of the Maven plugins used in the current");
            System.out.println("    POM file with the versions found in the repository");
            System.out.println("");
            System.out.println("Arguments:");
            System.out.println("  original-pom: location of the original POM");
            System.out.println("  target-file: where the cleaned POM will be written to");
            System.out.println("  pom-properties: property file where the POM properties will be written to");
            System.out.println("");
            System.out.println("Description:");
            System.out.println("  Cleans a Maven POM and prepare it for inclusion in the Debian");
            System.out.println("  repository for Maven.");
            System.out.println();
            System.out.println("  The POM will be normalised, and its parent tag removed if the option");
            System.out.println("  --no-parent is given. The version will be replaced by 'debian', unless");
            System.out.println("  a special rule applies (see below the discussion about rules).");
            System.out.println("  Build, profiles and other build time only sections of the POM");
            System.out.println("  will be stripped.");
            System.out.println();
            System.out.println("  If versions are given for a dependency, this version will be");
            System.out.println("  replaced by the 'debian' version, or a rule can be given to");
            System.out.println("  use a custom version");
            System.out.println();
            System.out.println("  You can modify those defaults with the help of the");
            System.out.println("  rules file. This file should contain the lines with the format:");
            System.out.println("  <groupId> [artifactId] [type] [version]");
            System.out.println("  where groupId, artifactId, type and version can be the explicit attribute to");
            System.out.println("  match, or can contain a wildcard (*) for generic matches.");
            System.out.println();
            System.out.println("  Each one of those elements can also be a replace rule, of the form");
            System.out.println("  s/<regex>/<replace>/ where regex is a regular expression, and replace");
            System.out.println("  is the replacement. Substitution groups $1 $2... can be used in the");
            System.out.println("  replacement if capture groups () have been used in the regex.");
            System.out.println();
            System.out.println("  The first element is mandatory (groupId), but you can ignore the following");
            System.out.println("  elements.");
            System.out.println("  If the version is missing, then any version will be replaced with 'debian'.");
            System.out.println("  If type is missing, then any type is matched.");
            System.out.println("  If artifactId is missing, then any artifactId is matched.");
            System.out.println();
            System.out.println("  You can also have comments in this file, it should be a line starting with #");
            System.out.println();
            System.out.println("  Example of a rules file:");
            System.out.println();
            System.out.println("  s/commons-(.*)/org.apache.commons.commons$1/");
            System.out.println("  org.itext * * s/1\\..*/1.x/");
            System.out.println("  org.itext * * s/2\\..*/2.x/");
            System.out.println("  org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/");
            System.out.println();
            System.out.println("  This rules file does the following:");
            System.out.println("  - all groupIds starting with commons- will have org.apache.commons. prefixed to them");
            System.out.println("  - any artifact in the org.itext group with a version number starting with 1. will");
            System.out.println("    use the 1.x version");
            System.out.println("  - any artifact in the org.itext group with a version number starting with 2. will");
            System.out.println("    use the 2.x version");
            System.out.println("  - the jar with groupId=org.codehaus.plexus and artifactId=plexus-container-default and a");
            System.out.println("    version starting with 1.0-alpha- will use the 1.0-alpha version");
            System.out.println();
            System.out.println("  The default rule (* * * s/.*/debian/) replaces any version number with");
            System.out.println("  the 'debian' version and always applies last if there was no other matches.");
            System.out.println();
            System.out.println("  Any rules given on the command line (using -R/--extra-rule,");
            System.out.println("  -U/--extra-published-rule or -I/--extra-ignore-rule) are applied");
            System.out.println("  in the order given, in between the rules given in the rules file");
            System.out.println("  (if any) and the default rules.");
            return;
        }
        POMCleaner cleaner = new POMCleaner();
        int i = inc(-1, args);
        boolean verbose = false;
        boolean noParent = false;
        boolean noRules = false;
        boolean hasPackageVersion = false;
        boolean keepPomVersion = false;
        boolean keepParentVersion = false;
        boolean keepAllElements = false;
        String debianPackage = "";
        String setVersion = null;
        File rulesFile = null;
        File publishedRulesFile = null;
        File mavenRepo = null;
        List<String> rulesExtra = new ArrayList<String>();
        List<String> publishedRulesExtra = new ArrayList<String>();
        List<String> ignoreRulesExtra = new ArrayList<String>();
        List<File> ignoreRulesFiles = new ArrayList<File>();
        while (i < args.length && (args[i].trim().startsWith("-") || args[i].trim().length() == 0)) {
            String arg = args[i].trim();
            if ("--verbose".equals(arg) || "-v".equals(arg)) {
                verbose = true;
            } else if ("--no-parent".equals(arg) || "-o".equals(arg)) {
                noParent = true;
            } else if ("--no-rules".equals(arg)) {
                noRules = true;
            } else if ("--no-publish-used-rule".equals(arg)) {
                cleaner.setPublishUsedRule(false);
            } else if ("--has-package-version".equals(arg) || "-h".equals(arg)) {
                hasPackageVersion = true;
            } else if ("--keep-pom-version".equals(arg)) {
                keepPomVersion = true;
            } else if ("--keep-parent-version".equals(arg)) {
                keepParentVersion = true;
            } else if ("--keep-all-elements".equals(arg)) {
                keepAllElements = true;
            } else if (arg.startsWith("--keep-elements=")) {
                String keepElements = arg.substring("--keep-elements=".length());
                StringTokenizer st = new StringTokenizer(keepElements, ",");
                while (st.hasMoreTokens()) {
                    cleaner.addElementToKeep(st.nextToken());
                }
            } else if (arg.startsWith("-p")) {
                debianPackage = arg.substring(2);
            } else if (arg.startsWith("--package=")) {
                debianPackage = arg.substring("--package=".length());
            } else if (arg.startsWith("-r")) {
                rulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--rules=")) {
                rulesFile = new File(arg.substring("--rules=".length()));
            } else if (arg.startsWith("-R")) {
                rulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-rule=")) {
                rulesExtra.add(arg.substring("--extra-rule=".length()));
            } else if (arg.startsWith("-u")) {
                publishedRulesFile = new File(arg.substring(2));
            } else if (arg.startsWith("--published-rules=")) {
                publishedRulesFile = new File(arg.substring("--published-rules=".length()));
            } else if (arg.startsWith("-U")) {
                publishedRulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-published-rule=")) {
                publishedRulesExtra.add(arg.substring("--extra-published-rule=".length()));
            } else if (arg.startsWith("-i") || arg.startsWith("--ignore-rules=")) {
                if (arg.startsWith("-i")) {
                    ignoreRulesFiles.add(new File(arg.substring(2)));
                } else {
                    ignoreRulesFiles.add(new File(arg.substring("--ignore-rules=".length())));
                }
            } else if (arg.startsWith("-I")) {
                ignoreRulesExtra.add(arg.substring(2));
            } else if (arg.startsWith("--extra-ignore-rule=")) {
                ignoreRulesExtra.add(arg.substring("--extra-ignore-rule=".length()));
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
        File originalPom = new File(args[i++].trim());
        File targetPom = new File(args[i++].trim());
        File pomProperties = new File(args[i++].trim());

        if (verbose) {
            System.out.println("Cleaning POM " + originalPom.getAbsolutePath() +
                    ", saving the result into " + targetPom.getAbsolutePath());
        }

        if (!noRules) {
            DependencyRuleSet rules = cleaner.getRulesFiles().get(RULES);
            rules.addAll(DependencyRuleSet.readRules(rulesFile, "", verbose, true));
            rules.addAll(rulesExtra);

            DependencyRuleSet ignoreRules = cleaner.getRulesFiles().get(IGNORE);
            for(File ignoreRulesFile : ignoreRulesFiles) {
                ignoreRules.addAll(DependencyRuleSet.readRules(ignoreRulesFile, "", verbose, true));
            }
            ignoreRules.addAll(ignoreRulesExtra);

            DependencyRuleSet publishedRules = cleaner.getRulesFiles().get(PUBLISHED);
            publishedRules.addAll(DependencyRuleSet.readRules(publishedRulesFile, "", verbose, true));
            publishedRules.addAll(publishedRulesExtra);

            cleaner.getRulesFiles().addDefaultRules();
        }

        if (mavenRepo != null) {
            Repository repository = new Repository(mavenRepo);
            cleaner.setRepository(repository);
            cleaner.usePluginVersionsFromRepository();
        }

        cleaner.setKeepAllElements(keepAllElements);
        cleaner.cleanPom(originalPom, targetPom, pomProperties, noParent, hasPackageVersion,
                keepPomVersion, keepParentVersion, setVersion, debianPackage);
    }

    private static int inc(int i, String[] args) {
        do {
            i++;
        } while (i < args.length && args[i].length() == 0);
        return i;
    }
}
