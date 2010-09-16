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

    public File getListOfPOMsFile() {
        return poms;
    }

    public void setListOfPOMsFile(File poms) {
        this.poms = poms;
        pomList = null;
        pomOptions = null;
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
                    handler.handlePOM(new File(pomPath), options.isNoParent());
                } catch (Exception e) {
                    log.log(Level.SEVERE, null, e);
                }
            }
        }
    }

    public POMOptions getPOMOptions(String pomPath) {
        return (POMOptions) getPomOptions().get(pomPath);
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
                out.println("# <path to pom file> [option]");
                out.println("# where option can be:");
                out.println("#   --ignore: ignore this POM or");
                out.println("#   --no-parent: remove the <parent> tag from the POM");
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
        private String destPackage;

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

        public String toString() {
            if (ignore) {
               return " --ignore";
            }
            String options = "";
            if (noParent) {
                options += " --no-parent ";
            }
            if (destPackage != null) {
                options += " --package=" + destPackage;
            }
            return options;
        }
    }
}
