--with jh\_maven\_repo\_helper:
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

This DH 7 plugin uses the tools provided by the maven\_repo\_helper
package to install the project jars and POM files into
/usr/share/maven-repo. It uses the file(s) debian/$package.poms for its
configuration, as well as the files debian/maven.rules,
debian/maven.ignoreRules and debian/maven.publishedRules.

It is designed to work alongside javahelper, and requires the use of the
javahelper plugin for DH 7 at the same time. To use this plugin in the
rules file, all you need to do is to add those lines to debian/rules:

::

    %:
        dh --with javahelper --with jh_maven_repo_helper $@

As for the plugin --with maven\_repo\_helper, you need to list the POM
files and the artifacts to install in debian/$package.poms, but this is
done in coordination with the configuration of javahelper. For example,
if the .jlibs configuration for javahelper installs the httpunit.jar
located in the lib folder into /usr/share/java,

libhttpunit-java.jlibs:

::

    lib/httpunit.jar

Then we need to tell javahelper to install this httpunit.jar file from
/usr/share/java into the Maven repository /usr/share/maven-repo

libhttpunit-java.poms:

::

    debian/pom.xml --usj-name=httpunit

