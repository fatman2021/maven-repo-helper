/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

/**
 *
 * @author ludo
 */
public class POMReaderTest extends TestBase {

    /**
     * Test of cleanPom method, of class POMReader.
     */
    public void testReadMavenPom() throws Exception {
        usePom("maven.xml");
        POMReader instance = new POMReader();
        POMInfo info = instance.readPom(pom);
        assertNotNull(info.getParent());
        assertEquals("org.apache.maven", info.getParent().getGroupId());
        assertEquals("maven-parent", info.getParent().getArtifactId());
        assertEquals("11", info.getParent().getVersion());
        assertEquals("pom", info.getParent().getType());

        assertEquals("org.apache.maven", info.getThisPom().getGroupId());
        assertEquals("maven", info.getThisPom().getArtifactId());
        assertEquals("2.1.0-SNAPSHOT", info.getThisPom().getVersion());
        assertEquals("pom", info.getThisPom().getType());

        assertEquals(1, info.getDependencies().size());
        Dependency dependency = (Dependency) info.getDependencies().get(0);
        assertEquals("junit", dependency.getGroupId());
        assertEquals("junit", dependency.getArtifactId());
        assertEquals("3.8.1", dependency.getVersion());
        assertEquals("jar", dependency.getType());

        assertEquals(34, info.getDependencyManagement().size());
        dependency = (Dependency) info.getDependencyManagement().get(0);
        assertEquals("org.apache.maven", dependency.getGroupId());
        assertEquals("maven-plugin-descriptor", dependency.getArtifactId());
        assertEquals("2.1.0-SNAPSHOT", dependency.getVersion());
        assertEquals("jar", dependency.getType());

        assertEquals(0, info.getProfileDependencies().size());
        assertEquals(0, info.getProfileDependencyManagement().size());

        assertEquals(14, info.getPluginManagement().size());
        dependency = (Dependency) info.getPluginManagement().get(0);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-jar-plugin", dependency.getArtifactId());
        assertEquals("2.1", dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(6, info.getPlugins().size());
        dependency = (Dependency) info.getPlugins().get(0);
        assertEquals("org.codehaus.mojo", dependency.getGroupId());
        assertEquals("clirr-maven-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(2, info.getProperties().size());
        assertEquals("2.1.0-SNAPSHOT", info.getProperties().get("mavenVersion"));
        assertEquals("1.0-beta-5", info.getProperties().get("wagonVersion"));

        assertNotNull(info.getModules());
        assertTrue(info.getModules().contains("maven-artifact"));
        assertTrue(info.getModules().contains("maven-core"));

    }

}