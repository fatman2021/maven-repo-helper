/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Properties;
import org.junit.Test;
import static org.custommonkey.xmlunit.XMLAssert.*;

/**
 *
 * @author ludo
 */
public class POMCleanerTest extends BaseTest {

    private File pomProperties;
    private File versionProperties;
    private File specialDependencies;

    @Override
    public void tearDown() {
        super.tearDown();
        if (specialDependencies != null && specialDependencies.exists()) {
            specialDependencies.delete();
            specialDependencies = null;
        }
        if (versionProperties != null && versionProperties.exists()) {
            versionProperties.delete();
            versionProperties = null;
        }
        if (pomProperties != null && pomProperties.exists()) {
            pomProperties.delete();
            pomProperties = null;
        }
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    @Test
    public void testCleanMavenPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        versionProperties = new File(testDir, "versions.properties");
        usePom("maven.xml");
        boolean inheritFromDebian = true;
        POMCleaner instance = new POMCleaner();
        ArrayList<Dependency> specialCases = new ArrayList<Dependency>();
        specialCases.add(new Dependency("org.apache.maven", "*", "*", "debian"));
        specialCases.add(new Dependency("org.apache.maven.reporting", "*", "jar", "debian"));
        specialCases.add(new Dependency("org.apache.maven.wagon", "*", "jar", "debian"));
        instance.cleanPom(pom, updatedPom, pomProperties, versionProperties, inheritFromDebian, specialCases);
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
        Properties pomInfo = new Properties();
        pomInfo.load(new FileReader(pomProperties));
        assertEquals("org.apache.maven", pomInfo.get("GROUP_ID"));
        assertEquals("maven", pomInfo.get("ARTIFACT_ID"));
        assertEquals("pom", pomInfo.get("TYPE"));
        assertEquals("2.1.0-SNAPSHOT", pomInfo.get("VERSION"));
        Properties versions = new Properties();
        versions.load(new FileReader(versionProperties));
        assertEquals("2.1.0-SNAPSHOT", versions.get("org.apache.maven.__.__.debian.version"));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    @Test
    public void testCleanModelloPom() throws Exception {
        pomProperties = new File(testDir, "pom.properties");
        versionProperties = new File(testDir, "versions.properties");
        usePom("modello-core.xml");
        boolean inheritFromDebian = false;
        POMCleaner instance = new POMCleaner();
        ArrayList<Dependency> specialCases = new ArrayList<Dependency>();
        specialCases.add(new Dependency("org.codehaus.modello", "*", "jar", "debian"));
        instance.cleanPom(pom, updatedPom, pomProperties, versionProperties, inheritFromDebian, specialCases);
        assertXMLEqual(read("modello-core.cleaned"), read(updatedPom));
        Properties versions = new Properties();
        versions.load(new FileReader(versionProperties));
        assertEquals("1.0-alpha-22", versions.get("org.codehaus.modello.__.jar.debian.version"));
    }

    /**
     * Test of main method, of class DebianPOM.
     */
    @Test
    public void testMain() throws Exception {
        specialDependencies = new File(testDir, "special-cases.txt");
        pomProperties = new File(testDir, "pom.properties");
        versionProperties = new File(testDir, "versions.properties");
        usePom("maven.xml");
        useFile("maven.spec", specialDependencies);
        String[] args = {"--debian-parent", pom.getAbsolutePath(), updatedPom.getAbsolutePath(),
            pomProperties.getAbsolutePath(), versionProperties.getAbsolutePath(), specialDependencies.getAbsolutePath()};
        POMCleaner.main(args);
        assertXMLEqual(read("maven.cleaned"), read(updatedPom));
        Properties versions = new Properties();
        versions.load(new FileReader(versionProperties));
        assertEquals("2.1.0-SNAPSHOT", versions.get("org.apache.maven.__.__.debian.version"));
    }
}