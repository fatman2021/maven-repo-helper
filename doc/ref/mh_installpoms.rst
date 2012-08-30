mh\_installpoms:
~~~~~~~~~~~~~~~~

Usage:

::

    mh_installpoms [option]...

Reads the file debian/$package.poms and installs each POM file listed in
the .poms file. Options:

::

    -h --help: show this text
    -V --version: show the version
    -p<package> --package=<package>: name of the Debian package which will contain this POM file
    -h --has-package-version: flag that indicates that this POM has the same version as the package, this helps packagers of depending packages
    -k --keep-pom-version: keep the original version of the POM but convert all other versions in dependencies and plugins.
      If there is a parent POM, keeps its version except when the parent is already registered in the Maven repository
    -e<version>, --set-version=<version>: set the version for the POM, do not use the version declared in the POM file.
    -r<rules> --rules=<rules>: path to the file containing the rules to apply when cleaning the POM.
      Optional, the default location is debian/maven.rules
    -u<rules> --published-rules=<rules>: path to the file containing the extra rules to publish in the property debian.mavenRules in the cleaned POM.
      Optional, the default location is debian/maven.publishedRules
    -i<rules> --ignore-rules=<rules>: path to the file containing the rules used to remove certain dependencies from the cleaned POM.
      Optional, the default location is debian/maven.ignoreRules
    -c<rules> --clean-ignore-rules=<rules>: path to the file containing the rules use to remove certain dependencies from the cleaned POM, in addition to the ignore rules specified previously.
      This is useful in  situations such as when the Maven clean target requires more  dependencies or plugins to ignore than the build target.
      Optional, it is ignored by default
    -s --no-rules: don't apply any rules for converting versions, do not even convert versions to the default 'debian' version
    --no-publish-used-rule: don't publish the rule used to transform a POM's own attributes in debian.mavenRules
    --keep-elements=<elem1,elem2>: keep the elements listed here even if they are normally removed by the clean operation. Such elements are build,reports,reporting,prerequisites,profiles.
    -m<repo root>--maven-repo=<repo root>: location of the Maven repository, used to force the versions of the Maven plugins used in the current POM file with the versions found in the repository
    -v --verbose: show more information while running
    -n --no-act: don't actually do anything, just print the results

debian/maven.rules is used to alter the version properties for the
library and its dependencies.

The file debian/$package.poms should contain the relative paths to the
POM files to install, each POM file in a separate line. After the path
to the POM file, you can add options which will influence the cleaning
of the POM file:

::

    --ignore: ignore this POM and its artifact if any
    --no-parent: remove the <parent> tag from the POM
    --package=<package>: an alternative package to use when installing this POM and its artifact
    --has-package-version: to indicate that the original version of the POM is the same as the upstream part of the version for the package.
    --keep-elements=<elem1,elem2>: a list of XML elements to keep in the POM during a clean operation with mh_cleanpom or mh\_installpom
    --artifact=<path>: path to the build artifact associated with this POM, it will be installed when using the command mh_install
    --java-lib: install the jar into /usr/share/java to comply with Debian packaging guidelines
    --usj-name=<name>: name to use when installing the library in /usr/share/java
    --usj-version=<version>: version to use when installing the library in /usr/share/java
    --no-usj-versionless: don't install the versionless link in /usr/share/java
    --dest-jar=<path>: the destination for the real jar it will be installed with mh_install.
    --classifier=<classifier>: Optional, the classifier for the jar. Empty by default.
    --ignore-pom: don't install the POM with mh\_install or mh\_installpoms. To use with POM files that are created temporarily for certain artifacts such as Javadoc jars.

For example, debian/$package.poms could contain:

::

    pom.xml --no-parent --has-package-version
    module1/pom.xml --has-package-version --keep-elements=profiles,reporting // keep the profiles and reporting elements in the POM, as in this case they can be used from another place
    module2/pom.xml --package=mypackage-doc // package this POM and associated items into mypackage-doc package

