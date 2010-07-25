/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.util.Iterator;

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

        assertEquals(0, info.getPlugins().size());
        assertEquals(0, info.getReportingPlugins().size());

        assertEquals(2, info.getProfilePlugins().size());
        dependency = (Dependency) info.getProfilePlugins().get(0);
        assertEquals("org.codehaus.mojo", dependency.getGroupId());
        assertEquals("clirr-maven-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(4, info.getProfileReportingPlugins().size());

        assertEquals(2, info.getProperties().size());
        assertEquals("2.1.0-SNAPSHOT", info.getProperties().get("mavenVersion"));
        assertEquals("1.0-beta-5", info.getProperties().get("wagonVersion"));

        assertNotNull(info.getModules());
        assertTrue(info.getModules().contains("maven-artifact"));
        assertTrue(info.getModules().contains("maven-core"));

    }

    public void testReadHibernateValidatorTckRunner() throws Exception {
        usePom("hibernate-validator-tck-runner.pom");
        POMReader instance = new POMReader();
        POMInfo info = instance.readPom(pom);
        assertNotNull(info.getParent());
        assertEquals("org.hibernate", info.getParent().getGroupId());
        assertEquals("hibernate-validator-parent", info.getParent().getArtifactId());
        assertEquals("4.0.2.GA", info.getParent().getVersion());
        assertEquals("pom", info.getParent().getType());

        assertEquals("org.hibernate", info.getThisPom().getGroupId());
        assertEquals("hibernate-validator-tck-runner", info.getThisPom().getArtifactId());
        assertEquals("4.0.2.GA", info.getThisPom().getVersion());
        assertEquals("jar", info.getThisPom().getType());

        assertEquals(6, info.getDependencies().size());
        Dependency dependency = (Dependency) info.getDependencies().get(5);
        assertEquals("org.jboss.test-harness", dependency.getGroupId());
        assertEquals("jboss-test-harness-jboss-as-51", dependency.getArtifactId());
        assertEquals("1.0.0", dependency.getVersion());
        assertEquals("jar", dependency.getType());

        assertEquals(0, info.getDependencyManagement().size());
        assertEquals(0, info.getProfileDependencies().size());
        assertEquals(0, info.getProfileDependencyManagement().size());
        assertEquals(0, info.getPluginManagement().size());

        assertEquals(3, info.getPlugins().size());
        dependency = (Dependency) info.getPlugins().get(1);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-surefire-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(2, info.getProfilePlugins().size());

        assertEquals(3, info.getProperties().size());
        assertEquals("/opt/java/jboss-5.1.0.GA", info.getProperties().get("jboss.home"));
        assertEquals("org.hibernate.validator.HibernateValidator", info.getProperties().get("validation.provider"));
        assertEquals("true", info.getProperties().get("remote.debug"));

        assertEquals(0, info.getModules().size());
    }

    public void testReadHibernateValidator() throws Exception {
        usePom("hibernate-validator.pom");
        POMReader instance = new POMReader();
        POMInfo info = instance.readPom(pom);
        assertNotNull(info.getParent());
        assertEquals("org.hibernate", info.getParent().getGroupId());
        assertEquals("hibernate-validator-parent", info.getParent().getArtifactId());
        assertEquals("4.0.2.GA", info.getParent().getVersion());
        assertEquals("pom", info.getParent().getType());

        assertEquals("org.hibernate", info.getThisPom().getGroupId());
        assertEquals("hibernate-validator", info.getThisPom().getArtifactId());
        assertEquals("4.0.2.GA", info.getThisPom().getVersion());
        assertEquals("jar", info.getThisPom().getType());

        assertEquals(6, info.getDependencies().size());
        Dependency dependency = (Dependency) info.getDependencies().get(0);
        assertEquals("javax.validation", dependency.getGroupId());
        assertEquals("validation-api", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("runtime", dependency.getScope());
        assertEquals(false, dependency.isOptional());

        dependency = (Dependency) info.getDependencies().get(1);
        assertEquals("org.slf4j", dependency.getGroupId());
        assertEquals("slf4j-api", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("runtime", dependency.getScope());
        assertEquals(false, dependency.isOptional());

        dependency = (Dependency) info.getDependencies().get(2);
        assertEquals("com.googlecode.jtype", dependency.getGroupId());
        assertEquals("jtype", dependency.getArtifactId());
        assertEquals("0.1.0", dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("runtime", dependency.getScope());
        assertEquals(false, dependency.isOptional());

        dependency = (Dependency) info.getDependencies().get(3);
        assertEquals("org.slf4j", dependency.getGroupId());
        assertEquals("slf4j-log4j12", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("runtime", dependency.getScope());
        assertEquals(true, dependency.isOptional());

        dependency = (Dependency) info.getDependencies().get(4);
        assertEquals("org.hibernate.java-persistence", dependency.getGroupId());
        assertEquals("jpa-api", dependency.getArtifactId());
        assertEquals("2.0.Beta-20090815", dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("runtime", dependency.getScope());
        assertEquals(true, dependency.isOptional());

        dependency = (Dependency) info.getDependencies().get(5);
        assertEquals("org.testng", dependency.getGroupId());
        assertEquals("testng", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("jar", dependency.getType());
        assertEquals("test", dependency.getScope());
        assertEquals(false, dependency.isOptional());
        assertEquals("jdk15", dependency.getClassifier());

        assertEquals(0, info.getDependencyManagement().size());
        assertEquals(2, info.getProfileDependencies().size());
        assertEquals(0, info.getProfileDependencyManagement().size());
        assertEquals(0, info.getPluginManagement().size());

        assertEquals(1, info.getReportingPlugins().size());
        dependency = (Dependency) info.getReportingPlugins().get(0);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-project-info-reports-plugin", dependency.getArtifactId());
        assertEquals("2.0.1", dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(9, info.getPlugins().size());
        dependency = (Dependency) info.getPlugins().get(0);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-source-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(1);
        assertEquals("org.codehaus.mojo", dependency.getGroupId());
        assertEquals("jaxb2-maven-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(2);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-surefire-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(3);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-surefire-report-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(4);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-shade-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(5);
        assertEquals("org.jboss.maven.plugins", dependency.getGroupId());
        assertEquals("maven-jdocbook-plugin", dependency.getArtifactId());
        assertEquals("2.2.0", dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(6);
        assertEquals("org.jboss.maven.plugins", dependency.getGroupId());
        assertEquals("maven-jdocbook-style-plugin", dependency.getArtifactId());
        assertEquals("2.0.0", dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(7);
        assertEquals("org.apache.maven.plugins", dependency.getGroupId());
        assertEquals("maven-assembly-plugin", dependency.getArtifactId());
        assertEquals(null, dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        dependency = (Dependency) info.getPlugins().get(8);
        assertEquals("org.twdata.maven", dependency.getGroupId());
        assertEquals("maven-cli-plugin", dependency.getArtifactId());
        assertEquals("0.6.3.CR3", dependency.getVersion());
        assertEquals("maven-plugin", dependency.getType());

        assertEquals(0, info.getProfilePlugins().size());
        assertEquals(1, info.getPluginDependencies().size());
        dependency = (Dependency) info.getPluginDependencies().get(0);
        assertEquals("org.hibernate", dependency.getGroupId());
        assertEquals("hibernate-jdocbook-style", dependency.getArtifactId());
        assertEquals("2.0.0", dependency.getVersion());
        assertEquals("jdocbook-style", dependency.getType());

        assertEquals(0, info.getProperties().size());
        assertEquals(0, info.getModules().size());
    }

}