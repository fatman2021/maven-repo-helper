package org.debian.maven;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.IOUtils;
import org.junit.rules.TemporaryFolder;

public class TemporaryPomFolder extends TemporaryFolder {

    private List<Reader> openedReaders = new ArrayList<Reader>();
    private File updatedPom;

    public String pomInUse;

    public File copyResource(String resource, File file) throws IOException {
        final FileWriter out = new FileWriter(file);
        final Reader in = read(resource);
        IOUtils.copy(in, out);
        in.close();
        out.close();
        return file;
    }

    public File usePom(String resource) throws IOException {
        pomInUse = resource;
        File pom = newFile("original.pom");
        return copyResource(resource, pom);
    }

    public Reader read(String resource) {
        Reader r = new InputStreamReader(this.getClass().getResourceAsStream("/" + resource));
        openedReaders.add(r);
        return r;
    }

    public File updatedPom() throws IOException {
        if(!(updatedPom instanceof File)) {
            updatedPom = newFile("updated.pom");
        }
        return updatedPom;
    }

    public Reader read(File f) throws FileNotFoundException {
        Reader r = new FileReader(f);
        openedReaders.add(r);
        return r;
    }

    @Override
    protected void after() {
        for (Iterator<Reader> i = openedReaders.iterator(); i.hasNext();) {
            Reader reader = i.next();
            try {
                reader.close();
            } catch (IOException ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            }
        }
        openedReaders.clear();
        updatedPom = null;
        super.after();
    }

    public static File getFileInClasspath(String resource) {
        if (!resource.startsWith("/")) {
            resource = "/" + resource;
        }
        URL url = TemporaryPomFolder.class.getResource(resource);
        File f;
        try {
            f = new File(url.toURI());
        } catch (URISyntaxException e) {
            f = new File(url.getPath());
        }
        return f;
    }

    public static String basename(String fileName) {
        String[] tokens = fileName.split("\\.(?=[^\\.]+$)");
        return tokens[0];
    }
}
