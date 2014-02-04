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

import java.util.StringTokenizer;

/**
 * The options associated to a pom file.
 */
public class POMOptions {

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

    public static POMOptions parse(String line) {
        StringTokenizer st = new StringTokenizer(line, " \t");
        
        POMOptions options = new POMOptions();
        
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
        
        return options;
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
