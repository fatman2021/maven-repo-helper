mh\_patchpom:
~~~~~~~~~~~~~

Usage:

::

    mh_patchpom [option]... [pom] [backup]

Patches one POM file using the Maven dependency rules.

Where:

::

    [pom] is the location of the POM file to transform. Default to pom.xml
    [backup] is the backup file for the pom. Default to pom.xml.save

Options:

::

    -h --help: show this text
    -V --version: show the version
    -p<package> --package=<package>: name of the Debian package containing this library
    -o --no-parent: don't inherit from a parent POM
    -k --keep-pom-version: keep the original version of the POM but convert all other versions in dependencies and plugins.
      If there is a parent POM, keeps its version except when the parent is already registered in the Maven repository
    -e<version>, --set-version=<version>: set the version for the POM, do not use the version declared in the POM file.
    -r<rules> --rules=<rules>: path to the file containing the rules to apply when cleaning the POM.
      Optional, the default location is debian/maven.rules
    -R<rule>, --extra-rule=<rule>: additional rule to apply when cleaning the POM. May occur multiple times, instead of or in addition to -r
    -u<rules> --published-rules=<rules>: path to the file containing the extra rules to publish in the property debian.mavenRules in the cleaned POM.
      Optional, the default location is debian/maven.publishedRules
    -U<rule>, --extra-published-rule=<rule>: extra rule to publish. May occur multiple times, instead of or in addition to -u
    -i<rules> --ignore-rules=<rules>: path to the file containing the rules used to remove certain dependencies from the cleaned POM.
      Optional, the default location is debian/maven.ignoreRules
    -I<rule>, --extra-ignore-rule=<rule>: additional rule used to remove dependencies from the transformed POM May occur multiple times, instead of or in addition to -i
    -c<rules> --clean-ignore-rules=<rules>: path to the file containing the rules use to remove certain dependencies from the cleaned POM, in addition to the ignore rules specified previously.
      This is useful in  situations such as when the Maven clean target requires more  dependencies or plugins to ignore than the build target.
      Optional, it is ignored by default
    -s --no-rules: don't apply any rules for converting versions, do not even convert versions to the default 'debian' version
    --no-publish-used-rule: don't publish the rule used to transform a POM's own attributes in debian.mavenRules
    -d --debian-build: transform during a Debian build, which means that some POM elements will be removed
    -b --build-no-docs: if the build doesn't need to build documentation, use this option to remove some POM elements (in particular plugins) which are useless here and may require extra dependencies and make the packaging harder.
    -m<repo root>--maven-repo=<repo root>: location of the Maven repository, used to force the versions of the Maven plugins used in the current POM file with the versions found in the repository
    -v --verbose: show more information while running
    -n --no-act: don't actually do anything, just print the results

