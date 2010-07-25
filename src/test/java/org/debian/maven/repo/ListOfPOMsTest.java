package org.debian.maven.repo;

/**
 * @author ludo
 */
public class ListOfPOMsTest extends TestBase {

    public void testRead() throws Exception {
        ListOfPOMs poms = new ListOfPOMs(getFileInClasspath("antlr3.poms"));
        assertEquals(6, poms.getPomOptions().size());
        assertTrue(poms.getPOMOptions("pom.xml").isNoParent());
        assertFalse(poms.getPOMOptions("runtime/Java/pom.xml").isNoParent());
        assertNull(poms.getPOMOptions("runtime/Java/pom.xml").getDestPackage());
        assertEquals("libantlr3-gunit-java", poms.getPOMOptions("gunit/pom.xml").getDestPackage());
        assertEquals("antlr3-gunit-maven-plugin", poms.getPOMOptions("gunit-maven-plugin/pom.xml").getDestPackage());
    }
}
