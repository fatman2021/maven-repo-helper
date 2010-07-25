/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.debian.maven.repo;

import junit.framework.TestCase;

/**
 *
 * @author ludo
 */
public class DependencyRuleTest extends TestCase {
    
    /**
     * Test of matches method, of class DependencyRule.
     */
    public void testMatches() {
        DependencyRule generic = new DependencyRule("");
        DependencyRule servlet23 = new DependencyRule("javax.servlet * * 2.3");
        DependencyRule log4j12 = new DependencyRule("log4j log4j jar s/1\\.2\\..*/1.2.x/");

        Dependency dependency = new Dependency("javax.servlet", "servlet-api", "jar", "2.3");
        assertTrue(generic.matches(dependency));
        assertTrue(servlet23.matches(dependency));
        assertFalse(log4j12.matches(dependency));
        
        Dependency dependencyNullVersion = new Dependency("javax.servlet", "servlet-api", "jar", null);
        assertTrue(generic.matches(dependencyNullVersion));
        assertFalse(servlet23.matches(dependencyNullVersion));
        assertFalse(log4j12.matches(dependencyNullVersion));
    }

    /**
     * Test of apply method, of class DependencyRule.
     */
    public void testApply() {
        Dependency dependency = new Dependency("javax.servlet", "servlet-api", "jar", "2.3");
        DependencyRule generic = new DependencyRule("");
        DependencyRule servlet23 = new DependencyRule("javax.servlet * * 2.3");
        assertEquals(new Dependency("javax.servlet", "servlet-api", "jar", "debian"), generic.apply(dependency));
        assertEquals(new Dependency("javax.servlet", "servlet-api", "jar", "2.3"), servlet23.apply(dependency));
    }

    /**
     * Test of compareTo method, of class DependencyRule.
     */
    public void testCompareTo() {
        DependencyRule generic = new DependencyRule("");
        DependencyRule servlet23 = new DependencyRule("javax.servlet * * 2.3");
        DependencyRule log4j12 = new DependencyRule("log4j log4j jar s/1\\.2\\..*/1.2.x/");
        assertTrue(generic.compareTo(servlet23) > 0);
        assertTrue(generic.compareTo(log4j12) > 0);
        assertTrue(servlet23.compareTo(log4j12) > 0);
        assertTrue(servlet23.compareTo(generic) < 0);
        assertTrue(log4j12.compareTo(servlet23) < 0);
        assertEquals(0, servlet23.compareTo(servlet23));
    }

}
