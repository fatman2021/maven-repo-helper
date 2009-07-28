/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

/**
 *
 * @author ludo
 */
public class POMTransformerTest extends TestBase {

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformMavenPom() throws Exception {
        usePom("maven.xml");
        boolean noParent = true;
        POMTransformer instance = new POMTransformer();
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
        POMTransformer instance = new POMTransformer();
        instance.addDefaultRules();
        instance.addRule(new DependencyRule("org.codehaus.plexus plexus-container-default jar s/1\\.0-alpha-.*/1.0-alpha/"));
        instance.transformPom(pom, updatedPom, noParent, false, null, "maven2");
        assertXMLEqual(read("maven-core.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformMavenJavadocPluginPom() throws Exception {
        usePom("maven-javadoc-plugin.xml");
        boolean noParent = true;
        POMTransformer instance = new POMTransformer();
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
        POMTransformer instance = new POMTransformer();
        instance.addDefaultRules();
        instance.transformPom(pom, updatedPom, noParent, false, null, "libmodello-java");
        assertXMLEqual(read("modello-core.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformPlexusContainerDefaultPom() throws Exception {
        usePom("plexus-container-default.xml");
        POMTransformer instance = new POMTransformer();
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
        POMTransformer instance = new POMTransformer();
        instance.addDefaultRules();
        instance.transformPom(pom, updatedPom, noParent, true, null, "libdoxia-java");
        assertXMLEqual(read("doxia-module-fml.transformed"), read(updatedPom));
    }

}