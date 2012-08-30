--with maven\_repo\_helper:
~~~~~~~~~~~~~~~~~~~~~~~~~~~

This DH 7 plugin uses the tools provided by the maven\_repo\_helper
package to install the project jars and POM files into
/usr/share/maven-repo. It uses the file(s) debian/$package.poms for its
configuration, as well as the files debian/maven.rules,
debian/maven.ignoreRules and debian/maven.publishedRules.

There can be several .poms file, one for each binary package if the
source package builds multiple binary packages, each containing its own
set of artifacts.

As --with maven\_repo\_helper plugin uses internally mh\_install, the
requirements for the configuration of the .poms files are the same,
namely:

For each POM file associated with a jar, we need to supply at least the
--artifact parameter. For example:

maven-debian-helper.poms:

::

    pom.xml --no-parent --has-package-version
    maven-debian-helper/pom.xml --has-package-version --artifact=maven-debian-helper/target/maven-debian-helper-*.jar --java-lib
    maven-debian-plugin/pom.xml --has-package-version --artifact=maven-debian-plugin/target/maven-debian-plugin-*.jar --java-lib
    maven-build-nodocs/pom.xml --has-package-version --artifact=maven-build-nodocs/target/maven-build-nodocs-*.jar --java-lib
    maven-packager-utils/pom.xml --has-package-version --artifact=maven-packager-utils/target/maven-packager-utils-*.jar --java-lib

To use this plugin in the rules file, all you need to do is to add those
lines to debian/rules:

::

    %:
        dh $@ --buildsystem=ant --with maven_repo_helper

The option --buildsystem=ant is optional, and you can use any other
build system or let DH 7 detect it, and you may add additional options
to dh.
