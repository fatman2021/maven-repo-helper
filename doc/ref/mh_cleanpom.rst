mh\_cleanpom:
~~~~~~~~~~~~~

Usage:

::

    mh_cleanpom [option]... [pom] [target] [pom-props]

Cleans the POM and prepare it for inclusion in the Maven repository.
Also extracts some information from the POM.

Where:

::

    [pom] is the location of the POM file to clean.
      Default to pom.xml or debian/pom.xml
    [target] is where the cleaned POM is written to.
      Default to debian/tmp/pom.xml
    [pom-props] is where the POM properties file will be written.
      Default to debian/tmp/pom.properties

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
    --keep-elements=<elem1,elem2>: keep the elements listed here even if they are normally removed by the clean operation. Such elements are build,reports,reporting,prerequisites,profiles.
    -m<repo root>--maven-repo=<repo root>: location of the Maven repository, used to force the versions of the Maven plugins used in the current POM file with the versions found in the repository
    -v --verbose: show more information while running
    -n --no-act: don't actually do anything, just print the results

Cleans a Maven POM and prepare it for inclusion in the Debian repository
for Maven.

The POM will be normalised, and its parent tag removed if the option
--no-parent is given. The version will be replaced by 'debian', unless a
special rule applies (see below the discussion about rules). Build,
profiles and other build time only sections of the POM will be stripped.

If versions are given for a dependency, this version will be replaced by
the 'debian' version, or a rule can be given to use a custom version.

You can modify those defaults with the help of the rules file. This file
should contain the lines with the format:

::

    <groupId> [artifactId] [type]  [version]  [classifier] [scope]

where groupId, artifactId, type, version, classifier and scope can be
the explicit attribute to match, or can contain a wildcard (\*) for
generic matches.

Each one of those elements can also be a replace rule, of the form
s/<regex>/<replace>/ where regex is a regular expression, and replace is
the replacement. Substitution groups $1 $2... can be used in the
replacement if capture groups () have been used in the regex.

The first element is mandatory (groupId), but you can ignore the
elements on the right hand side. If the scope is missing, then any scope
is matched and left unchanged. If the version missing, then any version
will be replaced with 'debian'. If type is missing, then any type is
matched and left unchanged. If artifactId is missing, then any
artifactId is matched and left unchanged.

You can also have comments in this file, it should be a line starting
with #

Example of a rules file:

::

    s/commons-(.*)/org.apache.commons.commons$1/  org.itext * * s/1\..*/1.x/ org.itext * *
    s/2\..*/2.x/ # use the alpha version of  plexus-container-default  org.codehaus.plexus
    plexus-container-default jar s/1\.0-alpha.*/1.0-alpha/

This rules file does the following:

-  all groupIds starting with commons- will have org.apache.commons.
   prefixed to them
-  any artifact in the org.itext group with a version number starting
   with 1. will use the 1.x version
-  any artifact in the org.itext group with a version number starting
   with 2. will use the 2.x version
-  the jar with groupId=org.codehaus.plexus and
   artifactId=plexus-container-default and a version starting with
   1.0-alpha- will use the 1.0-alpha version

The default rule (\* \* \* s/.\*/debian/ \*) replaces any version number
with the 'debian' version and always applies last if there was no other
matches. Another default rule (\* \* maven-plugin \* \*) keeps the
version for all plugins as the plugin mechanism requires a version in
Maven.

Any rules given on the command line (using -R/--extra-rule,
-U/--extra-published-rule or -I/--extra-ignore-rule) are applied in the
order given, in between the rules given in the rules file (if any) and
the default rules.

This script is used by mh\_installpom, normally you don't need to use it
directly, except if you need to investigate an issue with the cleaning
process for a POM file.
