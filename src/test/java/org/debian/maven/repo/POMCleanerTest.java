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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

import org.custommonkey.xmlunit.XMLUnit;
import org.debian.maven.TemporaryPomFolder;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xml.sax.SAXException;

import static org.junit.Assert.assertEquals;

import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
import static org.debian.maven.TemporaryPomFolder.basename;
import static org.debian.maven.repo.DependencyRuleSetFiles.RulesType.*;

public class POMCleanerTest {

    @Rule
    public TemporaryPomFolder tmpDir = new TemporaryPomFolder();

    private File pomProperties;
    private File specialDependencies;

    private POMCleaner instance;

    @Before
    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        instance = new POMCleaner();
    }

    @Test
    public void testCleanMavenPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("maven.xml");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "maven2");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven", pomInfo.get("groupId"));
        assertEquals("maven", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("2.1.0-SNAPSHOT", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanModelloPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("modello-core.xml");
        boolean noParent = false;
        instance.getRulesFiles().addDefaultRules();
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libmodello-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, false, true, true, null, "libmodello-java");
        assertXMLEqual(tmpDir.read("modello-core.keep.cleaned"), tmpDir.read(tmpDir.updatedPom()));
        pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.modello", pomInfo.get("groupId"));
        assertEquals("modello-core", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-22", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));

    }

    @Test
    public void testCleanWagonPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("wagon-http-lightweight.xml");
        boolean noParent = false;
        instance.getRulesFiles().addDefaultRules();
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libwagon-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache.maven.wagon", pomInfo.get("groupId"));
        assertEquals("wagon-http-lightweight", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-5", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanPlexusContainerDefaultPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("plexus-container-default.xml");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libplexus-container-default-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-container-default", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-9-stable-1", pomInfo.get("version"));
        assertEquals("1.0-alpha", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanPlexusActiveCollectionsPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("plexus-active-collections.pom");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha.*/1.0-alpha/"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-maven-plugin maven-plugin s/.*/1.3.8/"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libplexus-active-collections-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-active-collections", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-beta-2", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanPlexusArchiverPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("plexus-archiver.pom");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libplexus-archiver-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-archiver", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.0-alpha-12", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanSlf4jPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("slf4j.xml");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libslf4j-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.slf4j", pomInfo.get("groupId"));
        assertEquals("slf4j-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("1.5.6", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanCommonsValidatorPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("commons-validator.xml");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/3\\..*/3.x/"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libcommons-validator-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("commons-validator", pomInfo.get("groupId"));
        assertEquals("commons-validator", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("1.3.1", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanServletApiPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("servlet-api.pom");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("s/org.apache.tomcat/javax.servlet/ servlet-api jar s/.*/2.5/"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libservlet2.5-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("javax.servlet", pomInfo.get("groupId"));
        assertEquals("servlet-api", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("2.5", pomInfo.get("version"));
        assertEquals("2.5", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanHibernateValidatorParentPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("hibernate-validator-parent.pom");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.wagon wagon-webdav jar *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.jboss.maven.plugins maven-jdocbook-plugin maven-plugin *"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libhibernate-validator-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.hibernate", pomInfo.get("groupId"));
        assertEquals("hibernate-validator-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("4.0.2.GA", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanApacheParentPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("apache.pom");
        boolean noParent = true;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.apache apache-jar-resource-bundle * s/1\\..*/1.x/"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-archetype-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins changelog-maven-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-deploy-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-repository-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-scm-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-stage-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-eclipse-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-idea-plugin * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-source-plugin * *"));
        instance.addElementToKeep("build");
        instance.addElementToKeep("reporting");
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, false, false, false, null, "libmaven-parent-poms");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.apache", pomInfo.get("groupId"));
        assertEquals("apache", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("7", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanPlexusUtils2Pom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("plexus-utils2.pom");
        boolean noParent = false;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus-utils jar s/2\\..*/2.x/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.plexus plexus pom s/2\\..*/2.x/ * *"));        
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-release-plugin * *"));
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libplexus-utils2-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.plexus", pomInfo.get("groupId"));
        assertEquals("plexus-utils", pomInfo.get("artifactId"));
        assertEquals("jar", pomInfo.get("type"));
        assertEquals("2.0.5", pomInfo.get("version"));
        assertEquals("2.x", pomInfo.get("debianVersion"));
    }

    @Test
    public void testCleanMojoParentPom() throws Exception {
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("mojo-parent.pom");
        boolean noParent = false;
        instance.getRulesFiles().addDefaultRules();
        instance.getRulesFiles().get(RULES).add(new DependencyRule("junit junit jar s/3\\..*/3.x/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus codehaus-parent pom s/.*/debian/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.codehaus.mojo mojo-parent pom s/.*/debian/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.apache.maven maven-plugin-api jar s/2\\..*/2.x/ * *"));
        instance.getRulesFiles().get(RULES).add(new DependencyRule("org.apache.maven.doxia doxia-module-xhtml jar s/1\\..*/1.x/ * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-checkstyle-plugin * * * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-jxr-plugin * * * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.plugins maven-pmd-plugin * * * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.apache.maven.wagon wagon-webdav * * * *"));
        instance.getRulesFiles().get(IGNORE).add(new DependencyRule("org.codehaus.mojo cobertura-maven-plugin * * * *"));
        instance.addElementToKeep("build");
        instance.addElementToKeep("reporting");
        instance.cleanPom(pom, tmpDir.updatedPom(), pomProperties, noParent, true, false, false, null, "libmojo-parent-java");
        assertCleanedXMLEqual();
        Properties pomInfo = new Properties();
        pomInfo.load(new FileInputStream(pomProperties));
        assertEquals("org.codehaus.mojo", pomInfo.get("groupId"));
        assertEquals("mojo-parent", pomInfo.get("artifactId"));
        assertEquals("pom", pomInfo.get("type"));
        assertEquals("27", pomInfo.get("version"));
        assertEquals("debian", pomInfo.get("debianVersion"));
    }

    @Test
    public void testMain() throws Exception {
        specialDependencies = tmpDir.newFile("special-cases.txt");
        pomProperties = tmpDir.newFile("pom.properties");
        File pom = tmpDir.usePom("maven.xml");
        tmpDir.copyResource("maven.rules", specialDependencies);
        String[] args = {"--no-parent", "--has-package-version", "-pmaven2", "-r" + specialDependencies.getAbsolutePath(),
                pom.getAbsolutePath(), tmpDir.updatedPom().getAbsolutePath(), pomProperties.getAbsolutePath()};
        POMCleaner.main(args);
        assertCleanedXMLEqual();
    }

    private void assertCleanedXMLEqual() throws SAXException, IOException {
        assertXMLEqual(tmpDir.read(basename(tmpDir.pomInUse)+".cleaned"), tmpDir.read(tmpDir.updatedPom()));
    }
}