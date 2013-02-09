package org.debian.maven.repo;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;

import org.debian.maven.TemporaryPomFolder;
import org.junit.Test;

public class ListOfPOMsTest {

    @Test
    public void testRead() throws Exception {
        ListOfPOMs poms = new ListOfPOMs(TemporaryPomFolder.getFileInClasspath("antlr3.poms"));
        assertEquals(6, poms.getPomOptions().size());
        assertTrue(poms.getPOMOptions("pom.xml").isNoParent());
        assertFalse(poms.getPOMOptions("runtime/Java/pom.xml").isNoParent());
        assertNull(poms.getPOMOptions("runtime/Java/pom.xml").getDestPackage());
        assertEquals("libantlr3-gunit-java", poms.getPOMOptions("gunit/pom.xml").getDestPackage());
        assertEquals("antlr3-gunit-maven-plugin", poms.getPOMOptions("gunit-maven-plugin/pom.xml").getDestPackage());
    }
}
