mh\_installpom:
~~~~~~~~~~~~~~~

Usage:

::

    mh_installpom [option]... [pom]

Installs the POM file in /usr/share/maven-repo, at the correct location
for Maven. Before installing the POM, it prepares it with mh\_cleanpom.

Where:

::

    [pom] is the location of the POM file to install.
      GroupId, artifactId and version will be extracted from this file.

Options:

::

    -h --help: show this text
    -V --version: show the version
    -p<package> --package=<package>: name of the Debian package which will contain this POM file
    -h --has-package-version: flag that indicates that this POM has the same version as the package, this helps packagers of depending packages
    -o --no-parent: don't inherit from a parent POM
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
    -ignore-pom: read the POM but don't install it

This script installs the POM file in /usr/share/maven-repo, at the
correct location for Maven. Before installing the POM, it prepares it
with mh\_cleanpom.

debian/maven.rules is used to alter the version properties for the
library and its dependencies.

Prefer to use mh\_installpoms as it reuses the information defined in
debian/$package.poms and avoids repetition.
