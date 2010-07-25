package org.debian.maven.repo;

import java.io.File;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public interface POMHandler {

    void handlePOM(File pomFile, boolean noParent) throws Exception;

    void ignorePOM(File pomFile) throws Exception;
    
}
