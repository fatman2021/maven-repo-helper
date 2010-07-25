package org.debian.maven.repo;

/**
 * @author Ludovic Claude <ludovicc@users.sourceforge.net>
 */
public class DependencyNotFoundException extends Exception {
    private Dependency dependency;

    public DependencyNotFoundException(Dependency dependency) {
        this.dependency = dependency;
    }

    public Dependency getDependency() {
        return dependency;
    }

    public String getMessage() {
        return "Dependency not found " + dependency;
    }
}
