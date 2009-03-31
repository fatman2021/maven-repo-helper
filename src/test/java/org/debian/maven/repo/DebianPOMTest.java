/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.util.HashMap;
import java.util.Map;
import org.junit.Before;
import org.junit.Test;
import static org.custommonkey.xmlunit.XMLAssert.*;

/**
 *
 * @author ludo
 */
public class DebianPOMTest extends BaseTest {

    Map<String, String> libraryProperties;

    @Before
    @Override
    public void setUp() {
        super.setUp();
        libraryProperties = new HashMap<String, String>();
    }

    /**
     * Test of addLibrary method, of class DebianPOM.
     */
    @Test
    public void testAddLibrariesToNewDebianPOM() throws Exception {
        DebianPOM instance = new DebianPOM();
        libraryProperties.put("library.jar.debian.version", "1.0");
        libraryProperties.put("library.jar.debian.package", "libtest-java");
        instance.addLibrary(pom, updatedPom, libraryProperties);
        assertXMLEqual(read("new.pom"), read(pom));

        libraryProperties.put("library2.jar.debian.version", "3.0");
        libraryProperties.put("library2.jar.debian.package", "libother-java");
        instance.addLibrary(pom, updatedPom, libraryProperties);
        assertXMLEqual(read("new2.pom"), read(pom));
    }

    /**
     * Test of addLibrary method, of class DebianPOM.
     */
    @Test
    public void testUpdateLibraries() throws Exception {
        usePom("new.pom");
        DebianPOM instance = new DebianPOM();

        libraryProperties.put("library.jar.debian.version", "1.1");
        instance.addLibrary(pom, updatedPom, libraryProperties);
        assertXMLEqual(read("updated.pom"), read(pom));
    }

    /**
     * Test of removeLibrary method, of class DebianPOM.
     */
    @Test
    public void testRemoveLibrary() throws Exception {
        usePom("new2.pom");
        DebianPOM instance = new DebianPOM();

        instance.removeLibrary(pom, updatedPom, "library2.jar.debian.version", "library2.jar.debian.package");
        assertXMLEqual(read("new.pom"), read(pom));

        instance.removeLibrary(pom, updatedPom, "library.jar.debian.version", "library.jar.debian.package");
        assertXMLEqual(read("empty.pom"), read(pom));
    }

    /**
     * Test of main method, of class DebianPOM.
     */
    @Test
    public void testMain() throws Exception {
        String[] args = {"add", "tmp/original.pom", "library.jar.debian", "1.0", "libtest-java"};
        DebianPOM.main(args);
        assertXMLEqual(read("new.pom"), read(pom));

        args = new String[] {"add", "tmp/original.pom", "library2.jar.debian", "3.0", "libother-java"};
        DebianPOM.main(args);
        assertXMLEqual(read("new2.pom"), read(pom));

        args = new String[] {"remove", "tmp/original.pom", "library2.jar.debian"};
        DebianPOM.main(args);
        assertXMLEqual(read("new.pom"), read(pom));
    }

}