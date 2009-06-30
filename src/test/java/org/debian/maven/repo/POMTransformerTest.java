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
        instance.transformPom(pom, updatedPom, noParent, false, null);
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
        instance.transformPom(pom, updatedPom, noParent, false, "maven2");
        assertXMLEqual(read("maven-core.transformed"), read(updatedPom));
    }

    /**
     * Test of cleanPom method, of class POMCleaner.
     */
    public void testTransformModelloPom() throws Exception {
        usePom("modello-core.xml");
        boolean noParent = false;
        POMTransformer instance = new POMTransformer();
        instance.addDefaultRules();
        instance.transformPom(pom, updatedPom, noParent, false, "libmodello-java");
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
        instance.transformPom(pom, updatedPom, noParent, true, "libdoxia-java");
        assertXMLEqual(read("doxia-module-fml.transformed"), read(updatedPom));
    }

}