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
 * List of POM files used in the package.
 * 
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class ListOfPOMs {

    private static final Logger log = Logger.getLogger(ListOfPOMs.class.getName());

    private boolean verbose;

    /** The base directory */
    private File baseDir = new File(".");

    /** The <tt>debian/&lt;package>.poms</tt> file listing the pom files used in the package and their options */
    private File poms;

    /** The paths to the pom files, relatively to the base directory */
    private List<String> pomPaths;

    /** The options associated with each pom file. The pom file is specified as a path relative to the base directory */
    private Map<String, POMOptions> pomOptions;

    public ListOfPOMs() {
    }

    public ListOfPOMs(File poms) {
        this.poms = poms;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public void setListOfPOMsFile(File poms) {
        this.poms = poms;
    }

    /**
     * Returns the path of the first pom file defined.
     * 
     * @return the path of the first pom, or null if not found
     */
    public String getFirstPOM() {
        if (pomPaths == null) {
            readPomsFile();
        }
        if (!pomPaths.isEmpty()) {
            return pomPaths.get(0);
        }
        return null;
    }

    /**
     * Process the pom files with the specified handler.
     */
    public void foreachPoms(POMHandler handler) {
        if (pomPaths == null) {
            readPomsFile();
        }
        
        // process the ignored pom files
        for (String pomPath: pomPaths) {
            POMOptions options = getPOMOptions(pomPath);
            if (options.isIgnore()) {
                File pom = new File(baseDir, pomPath);
                try {
                    handler.ignorePOM(pom);
                } catch (Exception e) {
                    log.log(Level.SEVERE, "An error occured when processing the ignored pom file " + pom, e);
                }
            }
        }
        
        // process the included pom files
        for (String pomPath: pomPaths) {
            POMOptions options = getPOMOptions(pomPath);
            if (!options.isIgnore()) {
                File pom = new File(baseDir, pomPath);
                try {
                    handler.handlePOM(pom, options.isNoParent(), options.getHasPackageVersion());
                } catch (Exception e) {
                    log.log(Level.SEVERE, "An error occured when processing the pom file " + pom, e);
                }
            }
        }
    }

    /**
     * Returns the options associated to the specified pom file, or null if none exist.
     */
    public POMOptions getPOMOptions(File pom) {
        String pomRelPath = relativePath(pom);
        return getPOMOptions(pomRelPath);
    }

    /**
     * Returns the path of the specified file relatively to the base directory.
     */
    private String relativePath(File pom) {
        return pom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
    }

    /**
     * Returns the options associated to the specified pom file, or null if none exist.
     */
    public POMOptions getPOMOptions(String pomPath) {
        return getPomOptions().get(pomPath);
    }

    /**
     * Returns the options associated to the specified pom file.
     * The file is added to the list if not already present.
     */
    public POMOptions getOrCreatePOMOptions(File pom) {
        String pomRelPath = relativePath(pom);
        return getOrCreatePOMOptions(pomRelPath);
    }

    /**
     * Returns the options associated to the specified pom file.
     * The file is added to the list if not already present.
     */
    public POMOptions getOrCreatePOMOptions(String pomPath) {
        POMOptions options = getPOMOptions(pomPath);
        if (options == null) {
            options = addPOM(pomPath);
        }
        return options;
    }

    public Map<String, POMOptions> getPomOptions() {
        if (pomOptions == null) {
            readPomsFile();
        }
        return pomOptions;
    }

    /**
     * Add a pom file to the list.
     * 
     * @param pom the pom file to add
     * @return the default options associated to the pom
     */
    public POMOptions addPOM(File pom) {
        String pomRelPath = relativePath(pom);
        return addPOM(pomRelPath);
    }

    /**
     * Add a pom file to the list.
     * 
     * @param pomPath the path of the pom to add
     * @return the default options associated to the pom
     */
    public POMOptions addPOM(String pomPath) {
        if (pomPaths == null) {
            readPomsFile();
        }
        pomPaths.add(pomPath);
        POMOptions options = new POMOptions();
        pomOptions.put(pomPath, options);
        return options;
    }

    /**
     * Tells if this list of poms contains the specified pom file.
     */
    public boolean contains(File pomFile) {
        return pomFile.getAbsolutePath().startsWith(baseDir.getAbsolutePath()) && getPOMOptions(pomFile) != null;
    }

    /**
     * Parses the file containing the list of pom files.
     */
    private void readPomsFile() {
        if (pomPaths == null) {
            pomPaths = new ArrayList<String>();
            pomOptions = new HashMap<String, POMOptions>();
        }

        if (poms == null || !poms.exists()) {
            return;
        }

        try {
            if (verbose) {
                System.out.println("Read list of poms from " + poms.getAbsolutePath());
            }
            BufferedReader reader = new BufferedReader(new FileReader(poms));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (!st.hasMoreTokens() || line.startsWith("#")) {
                    continue;
                }
                
                String pomPath = st.nextToken();
                POMOptions options = addPOM(pomPath);
                
                // parse the options
                while (st.hasMoreTokens()) {
                    String option = st.nextToken().trim();
                    if ("--ignore".equals(option)) {
                        options.setIgnore(true);
                        break;
                    } else if ("--no-parent".equals(option)) {
                        options.setNoParent(true);
                    } else if (option.startsWith("--package=")) {
                        options.setDestPackage(option.substring("--package=".length()));
                    } else if ("--has-package-version".equals(option)) {
                        options.setHasPackageVersion(true);
                    } else if (option.startsWith("--keep-elements=")) {
                        options.setKeepElements(option.substring("--keep-elements=".length()));
                    } else if (option.startsWith("--artifact=")) {
                        options.setArtifact(option.substring("--artifact=".length()));
                    } else if ("--java-lib".equals(option)) {
                        options.setJavaLib(true);
                    } else if (option.startsWith("--usj-name=")) {
                        options.setUsjName(option.substring("--usj-name=".length()));
                    } else if (option.startsWith("--usj-version=")) {
                        options.setUsjVersion(option.substring("--usj-version=".length()));
                    } else if ("--no-usj-versionless".equals(option)) {
                        options.setNoUsjVersionless(true);
                    } else if (option.startsWith("--dest-jar=")) {
                        options.setDestJar(option.substring("--dest-jar=".length()));
                    } else if (option.startsWith("--classifier=")) {
                        options.setClassifier(option.substring("--classifier=".length()));
                    } else if (option.startsWith("--site-xml=")) {
                        options.setSiteXml(option.substring("--site-xml=".length()));
                    } else if ("--ignore-pom".equals(option)) {
                        options.setIgnorePOM(true);
                    }
                }
                
                if (verbose) {
                    System.out.println(pomPath + options);
                }
            }
            reader.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, "Unable to read the list of poms from " + poms, e);
        }
    }

    /**
     * Writes the file containing the list of pom files.
     */
    public void save() {
        if (poms != null) {
            try {
                if (pomPaths == null) {
                    readPomsFile();
                }
                PrintWriter out = new PrintWriter(new FileWriter(poms));
                out.println("# List of POM files for the package");
                out.println("# Format of this file is:");
                out.println("# <path to pom file> [option]*");
                out.println("# where option can be:");
                out.println("#   --ignore: ignore this POM and its artifact if any");
                out.println("#   --ignore-pom: don't install the POM. To use on POM files that are created");
                out.println("#     temporarily for certain artifacts such as Javadoc jars. [mh_install, mh_installpoms]");
                out.println("#   --no-parent: remove the <parent> tag from the POM");
                out.println("#   --package=<package>: an alternative package to use when installing this POM");
                out.println("#      and its artifact");
                out.println("#   --has-package-version: to indicate that the original version of the POM is the same as the upstream part");
                out.println("#      of the version for the package.");
                out.println("#   --keep-elements=<elem1,elem2>: a list of XML elements to keep in the POM");
                out.println("#      during a clean operation with mh_cleanpom or mh_installpom");
                out.println("#   --artifact=<path>: path to the build artifact associated with this POM,");
                out.println("#      it will be installed when using the command mh_install. [mh_install]");
                out.println("#   --java-lib: install the jar into /usr/share/java to comply with Debian");
                out.println("#      packaging guidelines");
                out.println("#   --usj-name=<name>: name to use when installing the library in /usr/share/java");
                out.println("#   --usj-version=<version>: version to use when installing the library in /usr/share/java");
                out.println("#   --no-usj-versionless: don't install the versionless link in /usr/share/java");
                out.println("#   --dest-jar=<path>: the destination for the real jar.");
                out.println("#     It will be installed with mh_install. [mh_install]");
                out.println("#   --classifier=<classifier>: Optional, the classifier for the jar. Empty by default.");
                out.println("#   --site-xml=<location>: Optional, the location for site.xml if it needs to be installed.");
                out.println("#     Empty by default. [mh_install]");
                out.println("#");
                for (String pomPath: pomPaths) {
                    out.println(pomPath + getPOMOptions(pomPath));
                }
                out.flush();
                out.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to write the list of poms " + poms, e);
            }
        }
    }

    /**
     * The options associated to a pom file.
     */
    public static class POMOptions {
        private boolean ignore;
        private boolean ignorePOM;
        private boolean noParent;
        private boolean hasPackageVersion;
        private String destPackage;
        private String keepElements;
        private String artifact;
        private boolean javaLib;
        private String usjName;
        private String usjVersion;
        private String destJar;
        private boolean noUsjVersionless;
        private String classifier;
        private String siteXml;

        public boolean isIgnore() {
            return ignore;
        }

        public void setIgnore(boolean ignore) {
            this.ignore = ignore;
        }

        public boolean isIgnorePOM() {
            return ignorePOM;
        }

        public void setIgnorePOM(boolean ignorePOM) {
            this.ignorePOM = ignorePOM;
        }

        public boolean isNoParent() {
            return noParent;
        }

        public void setNoParent(boolean noParent) {
            this.noParent = noParent;
        }

        public String getDestPackage() {
            return destPackage;
        }

        public void setDestPackage(String destPackage) {
            this.destPackage = destPackage;
        }

        public boolean getHasPackageVersion() {
            return hasPackageVersion;
        }

        public void setHasPackageVersion(boolean hasPackageVersion) {
            this.hasPackageVersion = hasPackageVersion;
        }

        public String getKeepElements() {
            return keepElements;
        }

        public void setKeepElements(String keepElements) {
            this.keepElements = keepElements;
        }

        public String getArtifact() {
            return artifact;
        }

        public void setArtifact(String artifact) {
            this.artifact = artifact;
        }

        public boolean isJavaLib() {
            return javaLib;
        }

        public void setJavaLib(boolean javaLib) {
            this.javaLib = javaLib;
        }

        public String getUsjName() {
            return usjName;
        }

        public void setUsjName(String usjName) {
            this.usjName = usjName;
        }

        public String getUsjVersion() {
            return usjVersion;
        }

        public void setUsjVersion(String usjVersion) {
            this.usjVersion = usjVersion;
        }

        public String getDestJar() {
            return destJar;
        }

        public void setDestJar(String destJar) {
            this.destJar = destJar;
        }

        public boolean isNoUsjVersionless() {
            return noUsjVersionless;
        }

        public void setNoUsjVersionless(boolean noUsjVersionless) {
            this.noUsjVersionless = noUsjVersionless;
        }

        public String getClassifier() {
            return classifier;
        }

        public void setClassifier(String classifier) {
            this.classifier = classifier;
        }

        public String getSiteXml() {
            return siteXml;
        }

        public void setSiteXml(String siteXml) {
            this.siteXml = siteXml;
        }

        public String toString() {
            if (ignore) {
               return " --ignore";
            }
            String options = "";
            if (noParent) {
                options += " --no-parent";
            }
            if (hasPackageVersion) {
                options += " --has-package-version";
            }
            if (destPackage != null) {
                options += " --package=" + destPackage;
            }
            if (keepElements != null) {
                options += " --keep-elements=" + keepElements;
            }
            if (artifact != null) {
                options += " --artifact=" + artifact;
            }
            if (javaLib) {
                options += " --java-lib";
            }
            if (usjName != null) {
                options += " --usj-name=" + usjName;
            }
            if (usjVersion != null) {
                options += " --usj-version=" + usjVersion;
            }
            if (destJar != null) {
                options += " --dest-jar=" + destJar;
            }
            if (noUsjVersionless) {
                options += " --no-usj-versionless";
            }
            if (classifier != null) {
                options += " --classifier=" + classifier;
            }
            if (siteXml != null) {
                options += " --site-xml=" + siteXml;
            }
            if (ignorePOM) {
                options += " --ignore-pom";
            }
            return options;
        }
    }
}
