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

import javax.xml.stream.XMLStreamException;

import org.debian.maven.TemporaryPomFolder;
import org.debian.maven.repo.DependencyRuleSetFiles.RulesType;
import org.junit.Test;
import org.junit.Rule;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class RepositoryTest {

    @Rule
    public TemporaryPomFolder tmpDir = new TemporaryPomFolder();

    @Test
    public void testScan() throws Exception {
        Repository repo = getRepository();
        repo.scan();

        assertEquals(25, repo.getResolvedPoms().size());
        assertEquals(0, repo.getUnresolvedPoms().size());
        assertEquals(0, repo.getPomsWithMissingParent().size());
        assertEquals(2, repo.getPomsWithMissingVersions().size());

        Dependency dependency = new Dependency("org.apache.ant", "ant-apache-bcel", "jar", "debian");
        POMInfo pom = repo.getPOM(dependency);
        assertNotNull(pom);
        assertEquals(pom.getThisPom(), dependency);

        assertEquals(pom, repo.searchMatchingPOM(dependency));
    }

    @Test
    public void testRegisterPom() throws Exception {
        Repository repo = getRepository();
        repo.scan();

        File pomFile = TemporaryPomFolder.getFileInClasspath("hibernate-validator.pom");
        POMInfo pom = getAntlrPom(repo, pomFile);
        try {
            repo.registerPom(pomFile, pom);
        } catch (DependencyNotFoundException ignore) {}

        assertEquals(25, repo.getResolvedPoms().size());
        assertEquals(1, repo.getUnresolvedPoms().size());
        assertEquals(1, repo.getPomsWithMissingParent().size());
        assertEquals(3, repo.getPomsWithMissingVersions().size());

        assertEquals(pom, repo.getPOM(pom.getThisPom()));
        assertEquals(pom, repo.searchMatchingPOM(pom.getThisPom()));
    }

    @Test
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

        File pomFile = TemporaryPomFolder.getFileInClasspath("antlr3-tools.xml");
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
        pomCleaner.getRulesFiles().addDefaultRules();
        pomCleaner.getRulesFiles().get(RulesType.RULES).add(new DependencyRule("org.antlr * * s/3\\..*/3.x/"));
        POMInfo pom = pomCleaner.transformPom(pomFile, tmpDir.updatedPom());
        return pom;
    }

    private Repository getRepository() {
        File baseDir = TemporaryPomFolder.getFileInClasspath("repository/root.dir");
        baseDir = baseDir.getParentFile();

        return new Repository(baseDir);
    }
}
