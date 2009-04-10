/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author ludo
 */
public class TestBase {

    protected static File testDir = new File("tmp");
    private List<Reader> openedReaders = new ArrayList();
    protected File pom;
    protected File updatedPom;

    @BeforeClass
    public static void setUpClass() throws Exception {
        testDir.mkdir();
        XMLUnit.setIgnoreWhitespace(true);
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        testDir.delete();
    }

    @Before
    public void setUp() {
        pom = new File(testDir, "original.pom");
        updatedPom = new File(testDir, "updated.pom");
    }

    @After
    public void tearDown() {
        if (pom.exists()) {
            pom.delete();
        }
        if (updatedPom.exists()) {
            updatedPom.delete();
        }
        for (Reader reader : openedReaders) {
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(TestBase.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        openedReaders.clear();
    }

    protected void useFile(String resource, File file) throws IOException {
        final FileWriter out = new FileWriter(file);
        final Reader in = read(resource);
        IOUtils.copy( in,out);
        in.close();
        out.close();
    }

    protected void usePom(String resource) throws IOException {
        useFile(resource, pom);
    }

    protected Reader read(String resource) {
        Reader r = new InputStreamReader(this.getClass().getResourceAsStream("/" + resource));
        openedReaders.add(r);
        return r;
    }

    protected Reader read(File f) throws FileNotFoundException {
        Reader r = new FileReader(f);
        openedReaders.add(r);
        return r;
    }

}