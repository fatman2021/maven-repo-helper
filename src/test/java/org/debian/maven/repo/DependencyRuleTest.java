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

import org.junit.Test;

import static org.debian.maven.repo.DependencyRuleSet.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DependencyRuleTest {

    /**
     * Test of matches method, of class DependencyRule.
     */
    @Test
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
    @Test
    public void testApply() {
        Dependency dependency = new Dependency("javax.servlet", "servlet-api", "jar", "2.3");
        DependencyRule generic = new DependencyRule("");
        DependencyRule servlet23 = new DependencyRule("javax.servlet * * 2.3");
        assertEquals(new Dependency("javax.servlet", "servlet-api", "jar", "debian"), generic.apply(dependency));
        assertEquals(new Dependency("javax.servlet", "servlet-api", "jar", "2.3"), servlet23.apply(dependency));
        
        dependency = new Dependency("com.google.inject", "guice", "jar", "3.0", null, false, "no_aop", null);
        DependencyRule rule = new DependencyRule("com.google.inject guice * s/.*/debian/ s/no_aop// *");
        assertEquals(new Dependency("com.google.inject", "guice", "jar", "debian", null, false, "", null), rule.apply(dependency));
    }

    /**
     * Test of compareTo method, of class DependencyRule.
     */
    @Test
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
        
        DependencyRule rule1 = new DependencyRule("* * * s/.*/debian/ * *");
        DependencyRule rule2 = new DependencyRule("s/org.eclipse.jetty.orbit/javax.servlet/ s/javax.servlet/servlet-api/ * s/.*/3.0/ * *");
        assertTrue(rule2.compareTo(rule1) < 0);
    }

    @Test
    public void testCompareGenericRules() {       
        DependencyRule rule1 = new DependencyRule("* * * s/.*/debian/ * *");
        DependencyRule rule2 = new DependencyRule("s/foo-(.*)/org.foo.$1/ * * s/.*/debian/ * *");
        assertTrue(rule2.compareTo(rule1) < 0);
    }

    @Test
    public void testEquals() {
        DependencyRule mavenPlugin = new DependencyRule("* * maven-plugin * * *");
        assertTrue(mavenPlugin.equals(MAVEN_PLUGINS_KEEP_VERSION_RULE));
    }
}
