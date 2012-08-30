mh\_linkjar:
~~~~~~~~~~~~

Usage:

::

    mh_linkjar [option]... [pom] [dest_jar] [link]...

Create symlinks for a jar installed by other means. The symlinks created
include links to the jar in /usr/share/maven-repo, at the correct
location for Maven. It can also create additional links to the jar,
usually located in /usr/share/java.

Where:

::

    [pom] is the location of the POM associated with the jar to install.
      GroupId, artifactId and version will be extracted from this file.
    [dest_jar] is the path of the installed jar, usually located in the usr/share/java folder.
    [link] is an additional link to the jar to install, usually there should be a link to usr/share/java/\$jar.jar and usr/share/java/\$jar-\$version.jar to comply with the Java packaging guidelines.
      Note that there is no need to specify those particular links if the --java-lib option is used.

Options:

::

    -h --help: show this text
    -V --version: show the version
    -p<package> --package=<package>: name of the Debian package which will contain this jar file
    -e<version>, --set-version=<version>: set the version for the jar, do not use the version declared in the POM file.
    -r<rules> --rules=<rules>: path to the file containing the rules to apply when cleaning the POM.
      Optional, the default location is debian/maven.rules
      Maven rules are used here to extract the groupId, artifactId and version from the POM file.
    -l --java-lib: Optional, if given it will install the jar into /usr/share/java to comply with the Debian Java specification.
      The jar will be installed as /usr/share/java/\$name-\$version.jar and a versionless link /usr/share/java/\$name.jar will point to it, as well as the links installed in /usr/share/maven-repo
    -n<name> --usj-name=<name>: Optional, the name to use when installing the library in /usr/share/java when --java-lib is used.
      Defaults to the artifact id found in the POM.
    -i<version> --usj-version=<version>: Optional, the version to use when installing the library in /usr/share/java when --java-lib is used.
      Defaults to the version found in the POM.
    -s --no-usj-versionless: Optional, don't install the versionless link in /usr/share/java.
      This flag is used only when the -l or --java-lib option is given.
    -c<classifier> --classifier=<classifier>: Optional, the classifier for the jar. Empty by default.
    -v --verbose: show more information while running
    -n --no-act: don't actually do anything, just print the results
    --skip-clean-pom: don't clean the pom, assume that a previous action ran mh_cleanpom with the correct options.
      mh_cleanpom is run only to extract the groupId, artifactId and version of the jar

