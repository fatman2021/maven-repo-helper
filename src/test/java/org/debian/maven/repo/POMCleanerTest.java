package org.debian.maven.repo;

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

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

public class POMCleanerTest extends TestBase {

    private File pomProperties;
    private File specialDependencies;

    private POMCleaner instance;

    public void setUp() {
        super.setUp();
        instance = new POMCleaner();
        instance.getRules().setRulesFile(null);
        instance.getIgnoreRules().setRulesFile(null);
        instance.getPublishedRules().setRulesFile(null);
    }


    public void tearDown() {
        super.tearDown();
        if (specialDependencies != null && specialDependencies.exists()) {
            specialDependencies.delete();
            specialDependencies = null;
        }
        if (pomProperties != null && pomProperties.exists()) {
            pomProperties.delete();
            pomProperties = null;
        }
    }

    public void testCleanMavenPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("maven.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "maven2");
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven", pomInfo.get("groupId"));
        assertEquals("maven", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("2.1.0-SNAPSHOT", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanModelloPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("modello-core.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libmodello-java");
        assertXMLEqual(read("modello-core.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, true, true, null, "libmodello-java");
        assertXMLEqual(read("modello-core.keep.cleaned"), read(updatedPom));
        pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

    }

    public void testCleanWagonPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("wagon-http-lightweight.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libwagon-java");
        assertXMLEqual(read("wagon-http-lightweight.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven.wagon", pomInfo.get("groupId"));
        assertEquals("wagon-http-lightweight", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-5", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanPlexusContainerDefaultPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-container-default.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libplexus-container-default-java");
        assertXMLEqual(read("plexus-container-default.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-container-default", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-9-stable-1", pomInfo.get("version"));
        assertEquals("1.0-alpha", pomInfo.get("debianVersion"));
    }

    public void testCleanPlexusActiveCollectionsPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-active-collections.pom");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha.*/1.0-alpha/"));
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-maven-plugin maven-plugin s/.*/1.3.8/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libplexus-active-collections-java");
        assertXMLEqual(read("plexus-active-collections.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-active-collections", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-2", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanPlexusArchiverPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-archiver.pom");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libplexus-archiver-java");
        assertXMLEqual(read("plexus-archiver.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-archiver", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-12", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanSlf4jPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("slf4j.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libslf4j-java");
        assertXMLEqual(read("slf4j.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.slf4j", pomInfo.get("groupId"));
        assertEquals("slf4j-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("1.5.6", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanCommonsValidatorPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("commons-validator.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libcommons-validator-java");
        assertXMLEqual(read("commons-validator.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("commons-validator", pomInfo.get("groupId"));
        assertEquals("commons-validator", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.3.1", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanServletApiPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("servlet-api.pom");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("s/org.apache.tomcat/javax.servlet/ servlet-api jar s/.*/2.5/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libservlet2.5-java");
        assertXMLEqual(read("servlet-api.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("javax.servlet", pomInfo.get("groupId"));
        assertEquals("servlet-api", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("2.5", pomInfo.get("version"));
        assertEquals("2.5", pomInfo.get("debianVersion"));
    }

    public void testCleanHibernateValidatorParentPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("hibernate-validator-parent.pom");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.wagon wagon-webdav jar *"));
        instance.addIgnoreRule(new DependencyRule("org.jboss.maven.plugins maven-jdocbook-plugin maven-plugin *"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libhibernate-validator-java");
        assertXMLEqual(read("hibernate-validator-parent.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.hibernate", pomInfo.get("groupId"));
        assertEquals("hibernate-validator-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("4.0.2.GA", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanApacheParentPom() throws Exception {

        pomProperties = new File(testDir, "pom.properties");
        usePom("apache.pom");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.apache apache-jar-resource-bundle * s/1\\..*/1.x/"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-archetype-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins changelog-maven-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-deploy-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-repository-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-scm-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-stage-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-eclipse-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-idea-plugin * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-source-plugin * *"));
        instance.addElementToKeep("build");
        instance.addElementToKeep("reporting");
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, false, false, null, "libmaven-parent-poms");
        assertXMLEqual(read("apache.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache", pomInfo.get("groupId"));
        assertEquals("apache", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("7", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testCleanPlexusUtils2Pom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-utils2.pom");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-utils jar s/2\\..*/2.x/ * *"));
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus pom s/2\\..*/2.x/ * *"));        
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libplexus-utils2-java");
        assertXMLEqual(read("plexus-utils2.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-utils", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("2.0.5", pomInfo.get("version"));
        assertEquals("2.x", pomInfo.get("debianVersion"));
    }

    public void testCleanMojoParentPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("mojo-parent.pom");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("junit junit jar s/3\\..*/3.x/ * *"));
        instance.addRule(new DependencyRule("org.codehaus codehaus-parent pom s/.*/debian/ * *"));
        instance.addRule(new DependencyRule("org.codehaus.mojo mojo-parent pom s/.*/debian/ * *"));
        instance.addRule(new DependencyRule("org.apache.maven maven-plugin-api jar s/2\\..*/2.x/ * *"));
        instance.addRule(new DependencyRule("org.apache.maven.doxia doxia-module-xhtml jar s/1\\..*/1.x/ * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-checkstyle-plugin * * * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-jxr-plugin * * * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.plugins maven-pmd-plugin * * * *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.wagon wagon-webdav * * * *"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo cobertura-maven-plugin * * * *"));
        instance.addElementToKeep("build");
        instance.addElementToKeep("reporting");
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, false, false, null, "libmojo-parent-java");
        assertXMLEqual(read("mojo-parent.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.mojo", pomInfo.get("groupId"));
        assertEquals("mojo-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("27", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    public void testMain() throws Exception {
        specialDependencies = new File(testDir, "special-cases.txt");
        pomProperties = new File(testDir, "pom.properties");
        usePom("maven.xml");
        useFile("maven.rules", specialDependencies);
        String[] args = {"--no-parent", "--has-package-version", "-pmaven2", "-r" + specialDependencies.getAbsolutePath(),
                pom.getAbsolutePath(), updatedPom.getAbsolutePath(), pomProperties.getAbsolutePath()};
        POMCleaner.main(args);
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
    }
}