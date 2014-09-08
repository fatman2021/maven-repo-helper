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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class RuleTest {
    
    /**
     * Test of match method, of class Rule.
     */
    @Test
    public void testMatch() {
        Rule simple = new Rule("test");
        assertFalse(simple.match("xxx"));
        assertTrue(simple.match("test"));
        assertFalse(simple.match("test2"));

        Rule simpleReplace = new Rule("s/test/foo/");
        assertFalse(simpleReplace.match("xxx"));
        assertTrue(simpleReplace.match("test"));
        assertFalse(simpleReplace.match("test2"));

        Rule complexReplace = new Rule("s/test(.*)/foo$1/");
        assertFalse(complexReplace.match("xxx"));
        assertTrue(complexReplace.match("test"));
        assertTrue(complexReplace.match("test2"));

        Rule complexReplace2 = new Rule("s/.*/debian/");
        assertTrue(complexReplace2.match("xxx"));
        assertTrue(complexReplace2.match("test"));
        assertTrue(complexReplace2.match("test2"));

        Rule generic = new Rule("*");
        assertTrue(generic.match("xxx"));
        assertTrue(generic.match("test"));
        assertTrue(generic.match("test2"));

        Rule generic2 = new Rule("test*");
        assertFalse(generic2.match("xxx"));
        assertTrue(generic2.match("test"));
        assertTrue(generic2.match("test2"));
    }

    /**
     * Test of apply method, of class Rule.
     */
    @Test
    public void testApply() {
        Rule simple = new Rule("test");
        assertEquals("test", simple.apply("test"));

        Rule simpleReplace = new Rule("s/test/foo/");
        assertEquals("foo", simpleReplace.apply("test"));

        Rule simpleReplace2 = new Rule("s/test/foo");
        assertEquals("foo", simpleReplace2.apply("test"));
        
        Rule erase = new Rule("s/test//");
        assertEquals("", erase.apply("test"));

        Rule complexReplace = new Rule("s/test(.*)/foo$1/");
        assertEquals("foo", complexReplace.apply("test"));
        assertEquals("foo2", complexReplace.apply("test2"));

        Rule complexReplace2 = new Rule("s/.*/debian/");
        assertEquals("debian", complexReplace2.apply("xxx"));
        assertEquals("debian", complexReplace2.apply("test"));
        assertEquals("debian", complexReplace2.apply("test2"));

        Rule generic = new Rule("*");
        assertEquals("xxx", generic.apply("xxx"));
        assertEquals("test", generic.apply("test"));
        assertEquals("test2", generic.apply("test2"));

        Rule generic2 = new Rule("test*");
        assertEquals("test", generic2.apply("test"));
        assertEquals("test2", generic2.apply("test2"));
    }

    /**
     * Test of apply method, of class Rule.
     */
    @Test
    public void testIsGeneric() {
        Rule simple = new Rule("test");
        assertFalse(simple.isGeneric());

        Rule simpleWithDot = new Rule("test.stuff");
        assertFalse(simpleWithDot.isGeneric());

        Rule simpleReplace = new Rule("s/test/foo/");
        assertFalse(simpleReplace.isGeneric());

        Rule simpleReplaceWithDot = new Rule("s/org.apache/org.debian/");
        assertFalse(simpleReplaceWithDot.isGeneric());
        
        Rule complexReplace = new Rule("s/test(.*)/foo$1/");
        assertTrue(complexReplace.isGeneric());

        Rule complexReplace2 = new Rule("s/.*/debian/");
        assertTrue(complexReplace2.isGeneric());

        Rule generic = new Rule("*");
        assertTrue(generic.isGeneric());

        Rule generic2 = new Rule("test*");
        assertTrue(generic2.isGeneric());
    }

    @Test
    public void testIsSubstitution() {
        assertFalse(new Rule("*").isSubstitution());
        assertFalse(new Rule("jar").isSubstitution());
        assertTrue(new Rule("s/foo/bar/").isSubstitution());
        assertTrue(new Rule("s/.*/debian/").isSubstitution());
    }
}
