/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.debian.maven.repo;

/**
 *
 * @author ludo
 */
public class Dependency {

    private String groupId;
    private String artifactId;
    private String type;
    private String version;

    public Dependency() {
    }

    public Dependency(String groupId, String artifactId, String type, String version) {
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.type = type;
        this.version = version;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Dependency other = (Dependency) obj;
        return true;
    }

    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.groupId != null ? this.groupId.hashCode() : 0);
        hash = 31 * hash + (this.artifactId != null ? this.artifactId.hashCode() : 0);
        hash = 31 * hash + (this.type != null ? this.type.hashCode() : 0);
        hash = 31 * hash + (this.version != null ? this.version.hashCode() : 0);
        return hash;
    }
}
