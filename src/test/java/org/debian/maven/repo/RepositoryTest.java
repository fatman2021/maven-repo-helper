package org.debian.maven.repo;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by IntelliJ IDEA.
 * User: ludo
 * Date: 5 juil. 2010
 * Time: 23:50:05
 * To change this template use File | Settings | File Templates.
 */
public class RepositoryTest extends TestBase {

    public void testScan() throws Exception {
        Repository repo = getRepository();
        repo.scan();

        assertEquals(8, repo.getResolvedPoms().size());
        assertEquals(0, repo.getUnresolvedPoms().size());
        assertEquals(0, repo.getPomsWithMissingParent().size());
        assertEquals(0, repo.getPomsWithMissingVersions().size());

        Dependency dependency = new Dependency("org.apache.ant", "ant-apache-bcel", "jar", "debian");
        POMInfo pom = repo.getPOM(dependency);
        assertNotNull(pom);
        assertEquals(pom.getThisPom(), dependency);

        assertEquals(pom, repo.searchMatchingPOM(dependency));
    }

    public void testRegisterPom() throws Exception {
        Repository repo = getRepository();
        repo.scan();

        File pomFile = getFileInClasspath("antlr3-tools.xml");
        POMInfo pom = getAntlrPom(repo, pomFile);
        try {
            repo.registerPom(pomFile, pom);
        } catch (DependencyNotFoundException ignore) {}

        assertEquals(8, repo.getResolvedPoms().size());
        assertEquals(1, repo.getUnresolvedPoms().size());
        assertEquals(1, repo.getPomsWithMissingParent().size());
        assertEquals(1, repo.getPomsWithMissingVersions().size());

        assertEquals(pom, repo.getPOM(pom.getThisPom()));
        assertEquals(pom, repo.searchMatchingPOM(pom.getThisPom()));
    }

    public void testSearchMatchingPOM() throws Exception {

        Repository repo = getRepository();
        repo.scan();

        Dependency antParentDep = new Dependency("org.apache.ant", "ant-parent", "pom", "1.7.1");
        assertEquals(antParentDep, repo.searchMatchingPOM(antParentDep).getThisPom());

        Dependency antParentDebianVersionDep = new Dependency("org.apache.ant", "ant-parent", "pom", "debian");
        assertEquals(antParentDebianVersionDep, repo.searchMatchingPOM(antParentDebianVersionDep).getThisPom());

        Dependency antParentDebianOtherVersionDep = new Dependency("org.apache.ant", "ant-parent", "pom", "1.8.3");
        assertEquals(antParentDebianVersionDep, repo.searchMatchingPOM(antParentDebianOtherVersionDep).getThisPom());

        Dependency antParentNoVersionDep = new Dependency("org.apache.ant", "ant-parent", "pom", null);
        assertEquals(antParentDebianVersionDep, repo.searchMatchingPOM(antParentNoVersionDep).getThisPom());

        Dependency antlrToolsDep = new Dependency("org.antlr", "antlr", "jar", "3.x");
        assertNull(repo.searchMatchingPOM(antlrToolsDep));

        File pomFile = getFileInClasspath("antlr3-tools.xml");
        POMInfo pom = getAntlrPom(repo, pomFile);
        try {
            repo.registerPom(pomFile, pom);
        } catch (DependencyNotFoundException ignore) {}

        Dependency antlrPom = new Dependency("org.antlr", "antlr", "jar", "3.x");
        assertEquals(antlrPom, repo.searchMatchingPOM(antlrPom).getThisPom());

        Dependency antlrVersionPom = new Dependency("org.antlr", "antlr", "jar", "3.2");
        assertEquals(antlrPom, repo.searchMatchingPOM(antlrVersionPom).getThisPom());

        Dependency antlrOtherVersionPom = new Dependency("org.antlr", "antlr", "jar", "3.3.1");
        assertEquals(antlrPom, repo.searchMatchingPOM(antlrOtherVersionPom).getThisPom());

        Dependency antlrNonMatchingVersionPom = new Dependency("org.antlr", "antlr", "jar", "2.0");
        assertNull(repo.searchMatchingPOM(antlrNonMatchingVersionPom));

        Dependency antlrNoVersionPom = new Dependency("org.antlr", "antlr", "jar", null);
        assertNull(repo.searchMatchingPOM(antlrNoVersionPom));

    }

    private POMInfo getAntlrPom(Repository repo, File pomFile) throws XMLStreamException, IOException {
        POMCleaner pomCleaner = new POMCleaner();
        pomCleaner.addDefaultRules();
        pomCleaner.addRule(new DependencyRule("org.antlr * * s/3\\..*/3.x/"));
        POMInfo pom = pomCleaner.transformPom(pomFile, updatedPom);
        return pom;
    }

    private Repository getRepository() {
        File baseDir = getFileInClasspath("repository/root.dir");
        baseDir = baseDir.getParentFile();

        return new Repository(baseDir);
    }
}
