mh\_install:
~~~~~~~~~~~~

Usage:

::

    mh_install [option]...

Reads the file debian/$package.poms and installs each POM file listed in
the .poms file, as well as the associated jars - assuming that at least
the --artifact option is given in the .poms file for all jars to
install.

Options:

::

    -h --help: show this text
    -V --version: show the version
    -p<package> --package=<package>: name of the Debian package which will contain all the files to install
    -e<version>, --set-version=<version>: set the version for all artifacts, do not use the version declared in the POM files.
    -r<rules> --rules=<rules>: path to the file containing the rules to apply when cleaning the POM files.
      Optional, the default location is debian/maven.rules
    -u<rules> --published-rules=<rules>: path to the file containing the extra rules to publish in the property debian.mavenRules in the cleaned POM.
      Optional, the default location is debian/maven.publishedRules
    -i<rules> --ignore-rules=<rules>: path to the file containing the rules used to remove certain dependencies from the cleaned POM.
      Optional, the default location is debian/maven.ignoreRules
    --no-publish-used-rule: don't publish the rule used to transform a POM's own attributes in debian.mavenRules
    -v --verbose: show more information while running
    -n --no-act: don't actually do anything, just print the results

The $package.poms file must contain enough information to locate the jar
files to install, and to associate them with their POM file. For each
POM file associated with a jar, we need to supply at least the
--artifact parameter. For example:

maven-debian-helper.poms:

::

    pom.xml --no-parent --has-package-version
    maven-debian-helper/pom.xml --has-package-version --artifact=maven-debian-helper/target/maven-debian-helper-*.jar --java-lib
    maven-debian-plugin/pom.xml --has-package-version --artifact=maven-debian-plugin/target/maven-debian-plugin-*.jar --java-lib
    maven-build-nodocs/pom.xml --has-package-version --artifact=maven-build-nodocs/target/maven-build-nodocs-*.jar --java-lib
    maven-packager-utils/pom.xml --has-package-version --artifact=maven-packager-utils/target/maven-packager-utils-*.jar --java-lib

Here we tell mh\_install to deploy the maven-debian-helper project and
its sub projects into /usr/share/maven-repo, and to deploy all jars also
into /usr/share/java (--java-lib option)

Internally, mh\_installpom is used to install the POM file,
mh\_installjar is used to install the jar if --artifact is supplied. If
a project or sub-project contains the file src/site/site.xml, then it
will be installed with mh\_installsite.

The file $package.poms contains the list of POM files and their releated
artifacts to install. The format of the $package.poms file is:

::

    <path to pom file> [option]*
    <path to second pom file> [option]*
    [...]

where option can be:

::

    --ignore: ignore this POM and its artifact if any
    --ignore-pom: don't install the POM. To use on POM files that are created
      temporarily for certain artifacts such as Javadoc jars. [mh_install, mh_installpoms]
    --no-parent: remove the <parent> tag from the POM
    --package=<package>: an alternative package to use when installing this POM
       and its artifact
    --has-package-version: to indicate that the original version of the POM is the same as the upstream part
       of the version for the package.
    --keep-elements=<elem1,elem2>: a list of XML elements to keep in the POM
       during a clean operation with mh_cleanpom or mh_installpom
    --artifact=<path>: path to the build artifact associated with this POM,
       it will be installed when using the command mh_install. [mh_install]
    --java-lib: install the jar into /usr/share/java to comply with Debian
       packaging guidelines
    --usj-name=<name>: name to use when installing the library in /usr/share/java
    --usj-version=<version>: version to use when installing the library in /usr/share/java
    --no-usj-versionless: don't install the versionless link in /usr/share/java
    --dest-jar=<path>: the destination for the real jar.
      It will be installed with mh_install. [mh_install]
    --classifier=<classifier>: Optional, the classifier for the jar. Empty by default.
    --site-xml=<location>: Optional, the location for site.xml if it needs to be installed.
      Empty by default. [mh_install]

