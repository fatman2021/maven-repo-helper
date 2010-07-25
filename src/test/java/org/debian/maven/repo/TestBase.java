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
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLTestCase;
import org.custommonkey.xmlunit.XMLUnit;

/**
 *
 * @author ludo
 */
public class TestBase extends XMLTestCase {

    protected static File testDir = new File("tmp");
    private List openedReaders = new ArrayList();
    protected File pom;
    protected File updatedPom;

    public void setUp() {
        XMLUnit.setIgnoreWhitespace(true);
        testDir.mkdir();
        pom = new File(testDir, "original.pom");
        updatedPom = new File(testDir, "updated.pom");
    }

    public void tearDown() {
        if (pom.exists()) {
            pom.delete();
        }
        if (updatedPom.exists()) {
            updatedPom.delete();
        }
        for (Iterator i = openedReaders.iterator(); i.hasNext(); ) {
            Reader reader = (Reader) i.next();
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(TestBase.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        openedReaders.clear();
        testDir.delete();
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

    protected File getFileInClasspath(String resource) {
        if (! resource.startsWith("/")) {
            resource = "/" + resource;
        }
        URL url = this.getClass().getResource(resource);
        File f;
        try {
          f = new File(url.toURI());
        } catch(URISyntaxException e) {
          f = new File(url.getPath());
        }
        return f;
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

    protected void show(File f) {
            try {
        LineNumberReader lnr = new LineNumberReader(read(f));
        do {
                String l = lnr.readLine();
                if (l == null) {
                    break;
                }
                System.out.println(l);
        } while (true);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
    }

}