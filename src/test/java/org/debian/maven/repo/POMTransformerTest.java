/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import org.custommonkey.xmlunit.XMLUnit;

import java.io.File;

/**
 *
 * @author ludo
 */
public class POMTransformerTest extends TestBase {

    private POMTransformer instance;

    public void setUp() {
        super.setUp();
        instance = new POMTransformer();
        instance.getRules().setRulesFile(null);
        instance.getIgnoreRules().setRulesFile(null);
        instance.getPublishedRules().setRulesFile(null);
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformMavenPom() throws Exception {
        usePom("maven.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.addRule(new DependencyRule("org.apache.maven.plugins maven-assembly-plugin maven-plugin s/.*/2.2/"));
        instance.transformPom(pom, updatedPom, noParent, false, null, null);
        assertXMLEqual(read("maven.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformMavenCorePom() throws Exception {
        usePom("maven-core.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.transformPom(pom, updatedPom, noParent, false, null, "maven2");
        assertXMLEqual(read("maven-core.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformMavenJavadocPluginPom() throws Exception {
        XMLUnit.setIgnoreComments(true);

        usePom("maven-javadoc-plugin.xml");
        boolean noParent = true;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.modello modello-maven-plugin maven-plugin s/.*/1.0.1/"));
        instance.addRule(new DependencyRule("org.codehaus.mojo clirr-maven-plugin * *"));
        instance.addRule(new DependencyRule("org.apache.bcel bcel jar s/5\\..*/5.x/"));
        instance.addRule(new DependencyRule("* maven-plugin-plugin maven-plugin s/.*/2.5/"));
        instance.transformPom(pom, updatedPom, noParent, false, null, null);
        assertXMLEqual(read("maven-javadoc-plugin.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformModelloPom() throws Exception {
        usePom("modello-core.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.transformPom(pom, updatedPom, noParent, false, null, "libmodello-java");
        assertXMLEqual(read("modello-core.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformPlexusContainerDefaultPom() throws Exception {
        usePom("plexus-container-default.xml");
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.transformPom(pom, updatedPom);
        assertXMLEqual(read("plexus-container-default.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformDoxiaFmlPom() throws Exception {
        usePom("doxia-module-fml.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.transformPom(pom, updatedPom, noParent, true, null, "libdoxia-java");
        assertXMLEqual(read("doxia-module-fml.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformAntlr3Pom() throws Exception {
        usePom("antlr3.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.addRule(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.addRule(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.addRule(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.addRule(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        instance.transformPom(pom, updatedPom, noParent, true, null, "libantlr3-java");
        assertXMLEqual(read("antlr3.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformAntlr3ParentPom() throws Exception {
        usePom("antlr3-parent.xml");
        boolean noParent = false;
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.addRule(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.addRule(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.addRule(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.addRule(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo buildnumber-maven-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("* maven-assembly-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.wagon * * *"));
        instance.addIgnoreModule(pom, "gunit");
        instance.addIgnoreModule(pom, "gunit-maven-plugin");

        instance.transformPom(pom, updatedPom, noParent, true, null, "libantlr3-java");
        assertXMLEqual(read("antlr3-parent.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformAntlr3ToolsPom() throws Exception {
        XMLUnit.setIgnoreComments(true);

        usePom("antlr3-tools.xml");
        boolean noParent = false;
        Repository repository = new Repository(new File("/usr/share/maven-repo"));
        instance.setRepository(repository);

        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.antlr stringtemplate * s/3\\..*/3.x/ *"));
        instance.addRule(new DependencyRule("antlr antlr jar s/2\\..*/2.x/ *"));
        instance.addRule(new DependencyRule("junit junit jar s/4\\..*/4.x/ *"));
        instance.addRule(new DependencyRule("org.antlr stringtemplate jar s/3\\..*/3.x/ *"));
        instance.addRule(new DependencyRule("org.antlr antlr3-maven-plugin maven-plugin s/.*/3.2/"));
        instance.usePluginVersionsFromRepository();
        //instance.addRule(new DependencyRule("org.codehaus.mojo antlr-maven-plugin maven-plugin s/.*/2.1/"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo findbugs-maven-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("org.codehaus.mojo buildnumber-maven-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("* maven-assembly-plugin maven-plugin *"));
        instance.addIgnoreRule(new DependencyRule("org.apache.maven.wagon * * *"));
        instance.addIgnoreModule(pom, "gunit");
        instance.addIgnoreModule(pom, "gunit-maven-plugin");

        instance.transformPom(pom, updatedPom, noParent, true, null, "libantlr3-java");
        assertXMLEqual(read("antlr3-tools.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformHivernateValidatorTckRunnerPom() throws Exception {
        usePom("hibernate-validator-tck-runner.pom");
        boolean noParent = false;
        Repository repository = new Repository(new File("/usr/share/maven-repo"));
        instance.setRepository(repository);

        instance.addDefaultRules();
        instance.usePluginVersionsFromRepository();

        instance.transformPom(pom, updatedPom, noParent, true, null, "libhibernate-validator-java");
        assertXMLEqual(read("hibernate-validator-tck-runner.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformHivernateValidatorPom() throws Exception {
        usePom("hibernate-validator.pom");
        boolean noParent = false;
        Repository repository = new Repository(new File("/usr/share/maven-repo"));
        instance.setRepository(repository);

        instance.addDefaultRules();
        instance.usePluginVersionsFromRepository();

        instance.transformPom(pom, updatedPom, noParent, true, null, "libhibernate-validator-java");
        assertXMLEqual(read("hibernate-validator.transformed"), read(updatedPom));
    }

}
