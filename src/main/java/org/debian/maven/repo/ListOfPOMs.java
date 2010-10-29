package org.debian.maven.repo;

import java.io.*;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class ListOfPOMs {

    private static final Logger log = Logger.getLogger(ListOfPOMs.class.getName());

    private boolean verbose;
    private File baseDir;
    private File poms;
    private List pomList;
    private Map pomOptions;

    public ListOfPOMs() {
    }

    public ListOfPOMs(File poms) {
        this.poms = poms;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public File getListOfPOMsFile() {
        return poms;
    }

    public void setListOfPOMsFile(File poms) {
        this.poms = poms;
    }

    public String getFirstPOM() {
        if (pomList == null) {
            readPomsFile();
        }
        if (!pomList.isEmpty()) {
            return (String) pomList.get(0);
        }
        return null;
    }

    public void foreachPoms(POMHandler handler) {
        if (pomList == null) {
            readPomsFile();
        }
        for (Iterator i = pomList.iterator(); i.hasNext(); ) {
            String pomPath = (String) i.next();
            POMOptions options = getPOMOptions(pomPath);
            if (options.isIgnore()) {
                try {
                    handler.ignorePOM(new File(pomPath));
                } catch (Exception e) {
                    log.log(Level.SEVERE, null, e);
                }
            }
        }
        for (Iterator i = pomList.iterator(); i.hasNext(); ) {
            String pomPath = (String) i.next();
            POMOptions options = getPOMOptions(pomPath);
            if (!options.isIgnore()) {
                try {
                    handler.handlePOM(new File(baseDir, pomPath), options.isNoParent(), options.getHasPackageVersion());
                } catch (Exception e) {
                    log.log(Level.SEVERE, null, e);
                }
            }
        }
    }

    public POMOptions getPOMOptions(File pom) {
        String pomRelPath = relativePath(pom);
        return getPOMOptions(pomRelPath);
    }

    private String relativePath(File pom) {
        return pom.getAbsolutePath().substring(baseDir.getAbsolutePath().length() + 1);
    }

    public POMOptions getPOMOptions(String pomPath) {
        return (POMOptions) getPomOptions().get(pomPath);
    }

    public POMOptions getOrCreatePOMOptions(File pom) {
        String pomRelPath = relativePath(pom);
        return getOrCreatePOMOptions(pomRelPath);
    }

    public POMOptions getOrCreatePOMOptions(String pomPath) {
        POMOptions options = getPOMOptions(pomPath);
        if (options == null) {
            options = addPOM(pomPath);
        }
        return options;
    }

    public Map getPomOptions() {
        if (pomOptions == null) {
            readPomsFile();
        }
        return pomOptions;
    }

    public POMOptions addPOM(File pom) {
        String pomRelPath = relativePath(pom);
        return addPOM(pomRelPath);
    }

    public POMOptions addPOM(String pomPath) {
        if (pomList == null) {
            readPomsFile();
        }
        pomList.add(pomPath);
        POMOptions options = new POMOptions();
        pomOptions.put(pomPath, options);
        return options;
    }

    private void readPomsFile() {
        if (pomList == null) {
            pomList = new ArrayList();
            pomOptions = new HashMap();
        }

        if (poms == null || !poms.exists()) {
            return;
        }

        try {
            if (verbose) {
                System.out.println("Read list of poms from " + poms.getAbsolutePath());
            }
            LineNumberReader reader = new LineNumberReader(new FileReader(poms));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                StringTokenizer st = new StringTokenizer(line, " \t");
                if (!st.hasMoreTokens() || line.startsWith("#")) {
                    continue;
                }
                String pom = st.nextToken();
                POMOptions options = addPOM(pom);
                while (st.hasMoreTokens()) {
                    String option = st.nextToken().trim();
                    if ("--ignore".equals(option)) {
                        options.setIgnore(true);
                        break;
                    } else if ("--no-parent".equals(option)) {
                        options.setNoParent(true);
                    } else if (option.startsWith("--package=")) {
                        options.setDestPackage(option.substring("--package=".length()));
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
                    }
                }
                if (verbose) {
                    System.out.println(pom + options);
                }
            }
            reader.close();
        } catch (IOException e) {
            log.log(Level.SEVERE, null, e);
        }
    }

    public void save() {
        if (poms != null && !poms.exists()) {
            try {
                PrintWriter out = new PrintWriter(new FileWriter(poms));
                out.println("# List of POM files for the package");
                out.println("# Format of this file is:");
                out.println("# <path to pom file> [option]*");
                out.println("# where option can be:");
                out.println("#   --ignore: ignore this POM or");
                out.println("#   --no-parent: remove the <parent> tag from the POM");
                out.println("#   --package=<package>: an alternative package to use when installing this POM");
                out.println("#      and its artifact");
                out.println("#   --keep-elements=<elem1,elem2>: a list of XML elements to keep in the POM");
                out.println("#      during a clean operation with mh_cleanpom or mh_installpom");
                out.println("#   --artifact=<path>: path to the build artifact associated with this POM,");
                out.println("#      it will be installed when using the command mh_install");
                out.println("#   --java-lib: install the jar into /usr/share/java to comply with Debian");
                out.println("#      packaging guidelines");
                out.println("#   --usj-name=<name>: name to use when installing the library in /usr/share/java");
                out.println("#   --usj-version=<version>: version to use when installing the library in /usr/share/java");
                out.println("#   --no-usj-versionless: don't install the versionless link in /usr/share/java");
                out.println("#   --dest-jar=<path>: the destination for the real jar");
                out.println("#");
                for (Iterator i = pomList.iterator(); i.hasNext();) {
                    String pomPath = (String) i.next();
                    out.println(pomPath + getPOMOptions(pomPath));
                }
                out.flush();
                out.close();
            } catch (Exception ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
    }

    public static class POMOptions {
        private boolean ignore;
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

        public boolean isIgnore() {
            return ignore;
        }

        public void setIgnore(boolean ignore) {
            this.ignore = ignore;
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
            return options;
        }
    }
}
