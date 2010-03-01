/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

/**
 *
 * @author ludo
 */
public class POMCleanerTest extends TestBase {

    private File pomProperties;
    private File specialDependencies;

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

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanMavenPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("maven.xml");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "maven2");
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven", pomInfo.get("groupId"));
        assertEquals("maven", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("2.1.0-SNAPSHOT", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanModelloPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("modello-core.xml");
        boolean noParent = false;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libmodello-java");
        assertXMLEqual(read("modello-core.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

        instance.cleanPom(pom, updatedPom, pomProperties, noParent, true, null, "libmodello-java");
        assertXMLEqual(read("modello-core.keep.cleaned"), read(updatedPom));
        pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanWagonPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("wagon-http-lightweight.xml");
        boolean noParent = false;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libwagon-java");
        assertXMLEqual(read("wagon-http-lightweight.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven.wagon", pomInfo.get("groupId"));
        assertEquals("wagon-http-lightweight", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-5", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanPlexusContainerDefaultPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-container-default.xml");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libplexus-container-default-java");
        assertXMLEqual(read("plexus-container-default.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-container-default", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-9-stable-1", pomInfo.get("version"));
        assertEquals("1.0-alpha", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanPlexusActiveCollectionsPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-active-collections.pom");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha.*/1.0-alpha/"));
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-maven-plugin maven-plugin s/.*/1.3.8/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libplexus-active-collections-java");
        assertXMLEqual(read("plexus-active-collections.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-active-collections", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-2", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanPlexusArchiverPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("plexus-archiver.pom");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libplexus-archiver-java");
        assertXMLEqual(read("plexus-archiver.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-archiver", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-12", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanSlf4jPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("slf4j.xml");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libslf4j-java");
        assertXMLEqual(read("slf4j.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.slf4j", pomInfo.get("groupId"));
        assertEquals("slf4j-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("1.5.6", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanCommonsValidatorPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("commons-validator.xml");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libcommons-validator-java");
        assertXMLEqual(read("commons-validator.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("commons-validator", pomInfo.get("groupId"));
        assertEquals("commons-validator", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.3.1", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testCleanServletApiPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        usePom("servlet-api.pom");
        boolean noParent = true;
        POMCleaner instance = new POMCleaner();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("s/org.apache.tomcat/javax.servlet/ servlet-api jar s/.*/2.5/"));
        instance.cleanPom(pom, updatedPom, pomProperties, noParent, false, null, "libservlet2.5-java");
        assertXMLEqual(read("servlet-api.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("javax.servlet", pomInfo.get("groupId"));
        assertEquals("servlet-api", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("2.5", pomInfo.get("version"));
        assertEquals("2.5", pomInfo.get("debianVersion"));
    }

    /**
     * Test of main method, of class DebianPOM.
     */
    public void testMain() throws Exception {
        specialDependencies = new File(testDir, "special-cases.txt");
        pomProperties = new File(testDir, "pom.properties");
        usePom("maven.xml");
        useFile("maven.rules", specialDependencies);
        String[] args = {"--no-parent", "-pmaven2", "-r" + specialDependencies.getAbsolutePath(),
            pom.getAbsolutePath(), updatedPom.getAbsolutePath(), pomProperties.getAbsolutePath() };
        POMCleaner.main(args);
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
    }
}