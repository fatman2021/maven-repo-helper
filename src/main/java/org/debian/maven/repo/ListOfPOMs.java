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
        init();
        
        if (!pomOptions.isEmpty()) {
            return pomOptions.keySet().iterator().next();
        }
        return null;
    }

    /**
     * Process the pom files with the specified handler.
     */
    public void foreachPoms(POMHandler handler) {
        init();
        
        // process the ignored pom files
        for (String pomPath: pomOptions.keySet()) {
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
        for (String pomPath: pomOptions.keySet()) {
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
        return getPOMOptions(relativePath(pom));
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
        return getOrCreatePOMOptions(relativePath(pom));
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
        init();
        
        return pomOptions;
    }

    /**
     * Add a pom file to the list.
     * 
     * @param pom the pom file to add
     * @return the default options associated to the pom
     */
    public POMOptions addPOM(File pom) {
        return addPOM(relativePath(pom));
    }

    /**
     * Add a pom file to the list.
     * 
     * @param pomPath the path of the pom to add
     * @return the default options associated to the pom
     */
    public POMOptions addPOM(String pomPath) {
        init();
        
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
    private void init() {
        if (pomOptions != null) {
            // the list is already initialized
            return;
        }
        
        pomOptions = new LinkedHashMap<String, POMOptions>();

        if (poms == null || !poms.exists()) {
            return;
        }
        
        if (verbose) {
            System.out.println("Loading the list of poms from " + poms.getAbsolutePath() + "...");
        }
        
        try {
            BufferedReader reader = new BufferedReader(new FileReader(poms));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim().replaceAll("\\s", " ");
                if (line.startsWith("#") || line.length() == 0) {
                    // skip comments and empty lines
                    continue;
                }
                
                String pomPath = line.contains(" ") ? line.substring(0, line.indexOf(" ")) : line;
                POMOptions options = POMOptions.parse(line.substring(pomPath.length()));
                
                pomOptions.put(pomPath, options);
                
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
                init();
                
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
                for (String pomPath: pomOptions.keySet()) {
                    out.println(pomPath + getPOMOptions(pomPath));
                }
                out.flush();
                out.close();
            } catch (Exception e) {
                log.log(Level.SEVERE, "Unable to write the list of poms " + poms, e);
            }
        }
    }
}
