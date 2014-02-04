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
import java.util.ArrayList;
import java.util.List;

import org.debian.maven.TemporaryPomFolder;
import org.junit.Test;

import static org.junit.Assert.*;

public class ListOfPOMsTest {

    @Test
    public void testRead() {
        ListOfPOMs poms = new ListOfPOMs(TemporaryPomFolder.getFileInClasspath("antlr3.poms"));
        assertEquals(6, poms.getPomOptions().size());
        assertTrue(poms.getPOMOptions("pom.xml").isNoParent());
        assertFalse(poms.getPOMOptions("runtime/Java/pom.xml").isNoParent());
        assertNull(poms.getPOMOptions("runtime/Java/pom.xml").getDestPackage());
        assertEquals("libantlr3-gunit-java", poms.getPOMOptions("gunit/pom.xml").getDestPackage());
        assertEquals("antlr3-gunit-maven-plugin", poms.getPOMOptions("gunit-maven-plugin/pom.xml").getDestPackage());
    }

    @Test
    public void testGetFirstPOM() {
        ListOfPOMs poms = new ListOfPOMs();
        assertNull("first pom", poms.getFirstPOM());
        
        poms = new ListOfPOMs();
        poms.setListOfPOMsFile(TemporaryPomFolder.getFileInClasspath("antlr3.poms"));
        assertEquals("pom.xml", poms.getFirstPOM());
        assertEquals("pom.xml", poms.getFirstPOM());
    }

    @Test
    public void testAddPom() {
        File pom = new File("pom.xml");
        ListOfPOMs poms = new ListOfPOMs();
        POMOptions options = poms.getPOMOptions(pom);
        assertNull(options);

        poms.addPOM(pom);
        options = poms.getPOMOptions(pom);
        assertNotNull(options);
    }

    @Test
    public void testGetOrCreatePOMOptions() {
        ListOfPOMs poms = new ListOfPOMs();
        POMOptions options = poms.getOrCreatePOMOptions(new File("pom.xml"));
        assertNotNull(options);
        assertNotNull(poms.getFirstPOM());
        
        POMOptions options2 = poms.getOrCreatePOMOptions(new File("pom.xml"));
        assertTrue(options == options2);
    }

    @Test
    public void testContains() {
        File pom = new File("./pom.xml");
        
        ListOfPOMs poms = new ListOfPOMs();
        poms.addPOM(pom);
        
        assertTrue(poms.contains(pom));
    }

    @Test
    public void testForeachPom() {
        ListOfPOMs poms = new ListOfPOMs(TemporaryPomFolder.getFileInClasspath("libplexus-components-java.poms"));
        
        final List<File> pomFiles = new ArrayList<File>();
        final List<File> ignoredPomFiles = new ArrayList<File>();
        
        poms.foreachPoms(new POMHandler() {
            @Override
            public void handlePOM(File pomFile, boolean noParent, boolean hasPackageVersion) throws Exception {
                pomFiles.add(pomFile);
            }

            @Override
            public void ignorePOM(File pomFile) throws Exception {
                ignoredPomFiles.add(pomFile);
            }
        });
        
        assertFalse(pomFiles.isEmpty());
        assertFalse(ignoredPomFiles.isEmpty());
    }
}
