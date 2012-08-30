`repository specification <repository.html>`_ \|
`reference <reference.html>`_ \| `tutorial <tutorial.html>`_

Packaging Java and Maven projects with Maven Repo Helper
========================================================

The maven-repo-helper package provides tools for installing and
maintaining Maven POMs and jars installed in /usr/share/maven-repo.

Maven is a comprehensive build tool for Java and other languages based
around the concept of plugins and a repository accessible normally from
internet (a la CPAN), and providing access to the libraries used by a
project.

To satisfy the requirement that all software must be built from the
Debian repository and that no access to Internet is allowed during a
build, we have re-created a Maven repository for Debian. This repository
is located in /usr/share/maven-repo and Maven POM files installed there
must follow some special requirements.

The Debian policy mandates that Java libraries are installed in
/usr/share/java, so we have many Java libraries have already been
packaged in Debian, but they don't have any Maven descriptors (POMs)
associated with them. maven-repo-helper can help by installing those
libraries also in /usr/share/maven-repo with their POM files.

The Maven repository maintained by Debian has some special
characteristics which make it difficult to use directly upstream POM
files without any changes. To allow a smooth upgrade of Java libraries,
we create both native and Debian versions of the artifacts, and replace
all versions in the dependencies with Debian versions - see the
`repository specification <repository.html>`_ document for more
informations about this process. In addition, entries in the POM file
which are not needed anymore, such as build information, are removed.
mh\_make provides the scripts mh\_patchpom and mh\_cleanpom which patch
dependencies versions and clean up the POM file respectively. Other
scripts often make use of those two scripts to provide more advanced
functionality.

maven-repo-helper has no dependency on Maven, so it can be used for any
library, including libraries used by Maven itself. It provides the
foundations for maven-debian-helper. Its goal is to manage the
/usr/share/maven-repo folder properly, and cannot be used alone to build
a Java library from source. You will normally use it either with Ant
(started by a cdbs rule or dh 7 rule or anything you like), or with
javahelper which will take care of building the binaries for your
project.

**Important:** If you want your build to use Maven itself, then run
mh\_make from the maven-debian-helper package on the upstream sources.
mh\_make will guide you for that process and generate all necessary
debian files for you. You can also use mh\_make --ant to speed up the
packaging of a project which uses Ant as the build tool and needs to
install its artifacts into the Maven repository.

Quick start
===========

This section presents some of the recommanded packaging styles:

DH 7 + Ant + maven-repo-helper
------------------------------

The build uses dh 7 style, it runs Ant for the build and uses
maven-repo-helper to install all jars both in /usr/share/java and in
/usr/share/maven-repo with the POM files.

debian/control:

::

    Source: bnd
    Section: java
    Priority: optional
    Maintainer: Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>
    Uploaders: Ludovic Claude <ludovicc@debian.org>
    Build-Depends: debhelper (>= 8), maven-repo-helper (>= 1.7), default-jdk
    Build-Depends-Indep: junit, libosgi-ee-minimum-api-java (>= 1.2.1)
    Standards-Version: 3.9.2
    Homepage: http://www.aqute.biz/Code/Bnd

    Package: bnd
    Architecture: all
    Depends: ${misc:Depends}
    Description: A tool to create and diagnose OSGi R4 bundles
     [...]

Note the Build-Depends on debhelper and maven-repo-helper

debian/rules:

::

    #!/usr/bin/make -f
    export JAVA_HOME=/usr/lib/jvm/default-java
    %:
        dh $@ --buildsystem=ant --with maven_repo_helper

We use here dh 7 for the build, with ant as the build system, and the
maven\_repo\_helper plugin which will install all generated artifacts in
the Maven repository. Ant is run with the upstream build.xml file and
the default target.

bnd.poms:

::

    biz.aQute.bndlib/tmp/pom.xml --has-package-version --artifact=biz.aQute.bndlib/tmp/biz.aQute.bndlib.jar --java-lib --usj-name=bndlib

Deploy the jar built in biz.aQute.bndlib/tmp folder (--artifact option)
as bndlib.jar (--usj-name option) in /usr/share/java (--java-lib option)
and deploy it as well in the Maven repository. We use the POM file
created by the build (biz.aQute.bndlib/tmp/pom.xml) as it contains the
metadata for the bndlib jar. The option --has-package-version tells
maven-repo-helper that the version of the POM matches the version of the
Debian package, which is a useful hint for mh\_make when packaging
another project which uses this library.

maven.rules:

::

    biz.aQute s/bnd/bndlib/

Change the name of the artifact from bnd to bndlib in the Maven POM. The
current version of bndlib will be replaced by a generic 'debian'
version.

You can use mh\_lspoms from the maven-debian-helper package to help you
locate POM files in your sources and generate both the .poms and
maven.rules files.

DH 7 + Ant + maven-ant-helper + maven-repo-helper
-------------------------------------------------

A variant of the previous solution, where we don't use the upstream
build script, but an Ant script which works on something which looks
like a Maven project and tries to emulate Maven as best as it can. This
is quite handy when you are working on a package used by Maven itself
and cannot use Maven during the build.

debian/control:

::

    Source: plexus-utils2
    Section: java
    Priority: optional
    Maintainer: Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>
    Uploaders: Ludovic Claude <ludovic.claude@laposte.net>
    Build-Depends: debhelper (>= 7), default-jdk, maven-repo-helper (>= 1.7), ant-optional, maven-ant-helper (>= 7) 
    Build-Depends-Indep: libplexus-parent-java (>= 2.0.6), junit (>= 3.8.2), default-jdk-doc 
    Standards-Version: 3.9.2
    Vcs-Svn: svn://svn.debian.org/svn/pkg-java/trunk/plexus-utils2
    Vcs-Browser: http://svn.debian.org/wsvn/pkg-java/trunk/plexus-utils2
    Homepage: http://plexus.codehaus.org/plexus-utils 

    Package: libplexus-utils2-java
    Architecture: all
    Depends: ${misc:Depends}, libplexus-parent-java (>= 2.0.6)
    Suggests: libplexus-utils2-java-doc
    Description: Plexus Common Utilities
     [...]

    Package: libplexus-utils2-java-doc
    Architecture: all
    Section: doc
    Depends: ${misc:Depends}, default-jdk-doc
    Suggests: libplexus-utils2-java
    Description: Documentation for Plexus Common Utilities
     [...]

We define a binary package which will contain the jars and the fragment
of the Maven repository for this package (libplexus-utils2-java), and a
documentation package containing the Javadoc folder (installed in
/usr/share/doc) and the Javadoc jar installed in the Maven repository
(libplexus-utils2-java-doc).

Note the Build-Depends on debhelper, maven-repo-helper and
maven-ant-helper.

debian/rules:

::

    #!/usr/bin/make -f

    DEB_SOURCE_PACKAGE   := $(shell dpkg-parsechangelog | grep '^Source:' | cut -f 2 -d ' ')
    DEB_UPSTREAM_VERSION := $(shell dpkg-parsechangelog | egrep '^Version:' | cut -f 2 -d ' ' | cut -d: -f2- | sed 's/-[^-]*$$//')

    export JAVA_HOME=/usr/lib/jvm/default-java
    export CLASSPATH=$(shell for jar in `cat debian/build-classpath`; do if [ -f "$$jar" ]; then echo -n "$${jar}:"; fi; done)
    export ANT_ARGS=-Dbasedir=$(realpath .) -Dpackage=$(DEB_SOURCE_PACKAGE) -f /usr/share/maven-ant-helper/maven-build.xml

    %:
        dh $@ --buildsystem=ant --with maven_repo_helper

We use here dh 7 for the build, with ant as the build system, and the
maven\_repo\_helper plugin which will install all generated artifacts in
the Maven repository We configure ant to use the maven-ant-helper build
script (/usr/share/maven-ant-helper/maven-build.xml) and the classpath
defined in debian/build-classpath

libplexus-utils2-java.poms:

::

    pom.xml --has-package-version --artifact=target/plexus-utils-*.jar --java-lib --usj-name=plexus-utils2

Install the jar as plexus-utils2.jar (--usj-name option) in
/usr/share/java (--java-lib option) and install it as well in
/usr/share/maven-repo with its POM file. The jar is coming from the
target/ folder used by Maven (and the maven-repo-helper script) for its
builds, and we use a glob pattern (\*) to match any version of the jar
(--artifact option). The option --has-package-version tells
maven-repo-helper that the version of the POM matches the version of the
Debian package, which is a useful hint for mh\_make when packaging
another project which uses this library.

libplexus-utils2-java-doc.poms:

::

    target/plexus-utils.javadoc.pom --has-package-version --artifact=target/plexus-utils.javadoc.jar --classifier=javadoc --ignore-pom

Install the javadoc jar into /usr/share/maven-repo, without a POM file
(--ignore-pom option) because that's the way Maven works - javadoc jars
have no dedicated POM files for them. Instead, there is a classifier
which tells Maven that this jar contains Javadoc (--classifier option).
The javadoc jar is packaged in libplexus-utils2-java-doc package.

libplexus-utils2-java-doc.install:

::

    target/apidocs/* usr/share/doc/libplexus-utils2-java/api

Install the javadoc folder into
/usr/share/doc/libplexus-utils2-java/api. This folder is packaged in
libplexus-utils2-java-doc package.

maven.rules:

::

    junit junit jar s/3\..*/3.x/ * *
    org.codehaus.plexus plexus-utils jar s/2\..*/2.x/ * *
    org.codehaus.plexus plexus pom s/2\..*/2.x/ * *

Adapt versions for Debian in the Maven POMs, as it ease small upgrades.

-  junit:junit jar dependencies with a version starting with 3. will use
   version 3.x instead (and libjunit-java contains a similar rule which
   install version 3.x in the Maven repository)
-  org.codehaus.plexus:plexus-utils jar dependencies with a version
   starting with 2. will use version 2.x instead
-  org.codehaus.plexus:plexus pom dependencies with a version starting
   with 2. will use version 2.x instead

DH 7 + javahelper + maven-repo-helper
-------------------------------------

debian/control:

::

    Source: httpunit
    Maintainer: Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>
    Uploaders: Miguel Landaeta <miguel@miguel.cc>, Michael Koch <konqueror@gmx.de>
    Section: java
    Priority: optional
    Build-Depends: default-jdk, ant (>= 1.7.0-6), javahelper (>= 0.28),
     debhelper (>= 7.0.50~), rhino (>= 1.7R1-2), libjtidy-java (>= 7+svn20070309-2),
     libgnujaf-java (>= 1.1.1-4), junit (>= 3.8.2), libgnumail-java (>= 1.1.2-2),
     libservlet2.4-java (>= 5.0.30-8), libxerces2-java (>= 2.9.1-2),
     libnekohtml-java (>= 0.9.5+dfsg-1.1), maven-repo-helper (>= 1.7)
    Standards-Version: 3.9.2
    Homepage: http://httpunit.org/
    Vcs-Browser: http://svn.debian.org/wsvn/pkg-java/trunk/httpunit/
    Vcs-Svn: svn://svn.debian.org/svn/pkg-java/trunk/httpunit

    Package: libhttpunit-java
    Architecture: all
    Section: java
    Priority: optional
    Depends: ${java:Depends}, libxerces2-java (>= 2.9.1-2), libnekohtml-java (>= 0.9.5+dfsg-1.1),
     rhino (>= 1.7R1-2), libjtidy-java (>= 7+svn20070309-2), libservlet2.4-java
     (>= 5.0.30-8), ${misc:Depends}
    Recommends: junit (>= 3.8.2)
    Suggests: libhttpunit-java-doc (= ${binary:Version})
    Description: automated web site testing toolkit
        [...]

Note the Build-Depends on debhelper, javahelper, ant and
maven-repo-helper

debian/rules:

::

    #!/usr/bin/make -f
    %:
        dh --with javahelper --with jh_maven_repo_helper $@

We use here dh 7 for the build, with javahelper as the build system
(which in turn will launch Ant because build.xml exists), and the
jh\_maven\_repo\_helper plugin which is adapted to work with javahelper
and will install all generated artifacts in the Maven repository.

libhttpunit-java.jlibs:

::

    lib/httpunit.jar

We tell javahelper to install the httpunit.jar file located in lib into
/usr/share/java

libhttpunit-java.poms:

::

    debian/pom.xml --usj-name=httpunit

Create a link for the jar /usr/share/java/httpunit.jar into the Maven
repository and use debian/pom.xml as its companion metadata file

CDBS + maven-repo-helper using mh\_install
------------------------------------------

debian/control:

::

    Source: maven-debian-helper
    Section: java
    Priority: optional
    Maintainer: Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>
    Uploaders: Torsten Werner <twerner@debian.org>, Ludovic Claude <ludovic.claude@laposte.net>,
     Michael Koch <konqueror@gmx.de>, Damien Raude-Morvan <drazzib@debian.org>
    Build-Depends: cdbs, debhelper (>= 7), default-jdk, maven-ant-helper (>= 7), maven-repo-helper (>= 1.7), ant-optional, help2man
    Build-Depends-Indep: junit, libmaven-plugin-tools-java, libplexus-velocity-java
    Standards-Version: 3.9.2
    Vcs-Svn: svn://svn.debian.org/svn/pkg-java/trunk/maven-debian-helper
    Vcs-Browser: http://svn.debian.org/wsvn/pkg-java/trunk/maven-debian-helper/
    Homepage: http://wiki.debian.org/Java/MavenBuilder

    Package: maven-debian-helper
    Architecture: all
    Depends: ${misc:Depends}, default-jdk, maven-repo-helper (>= 1.5), maven2 (>= 2.2.1-5),
     libmaven-clean-plugin-java, libmaven-resources-plugin-java,
     libmaven-compiler-plugin-java, libmaven-jar-plugin-java, libmaven-site-plugin-java,
     libsurefire-java, velocity, libplexus-velocity-java
    Suggests: libmaven-javadoc-plugin-java, apt-file, devscripts, subversion
    Description: Helper tools for building Debian packages with Maven
     [...]

Note the Build-Depends on cdbs and maven-repo-helper. maven-ant-helper
and ant-optional are also needed for this package, as it's quite complex
but this is not important for this tutorial.

debian/rules:

::

    #!/usr/bin/make -f
    include /usr/share/cdbs/1/rules/debhelper.mk
    include /usr/share/cdbs/1/class/ant.mk

    PACKAGE              := $(DEB_SOURCE_PACKAGE)
    VERSION              := $(shell echo ${DEB_UPSTREAM_VERSION} | sed -r 's/([0-9\.]+).*/\1/')
    JAVA_HOME            := /usr/lib/jvm/default-java
    DEB_JARS             := ant-nodeps classworlds maven-core maven-artifact maven-artifact-manager maven-model \
     maven-compat maven-plugin-api maven-project maven-scm-api velocity file-management plexus-utils \
     plexus-container-default-alpha maven-repo-helper
    DEB_ANT_BUILD_TARGET := package
    DEB_ANT_BUILDFILE    := debian/build.xml
    DEB_ANT_ARGS         := -Dpackage=$(PACKAGE) -Dbin.package=$(PACKAGE)
    API_DOCS             := target/api

    install/$(PACKAGE)::
        mh_install -p$(PACKAGE)

    clean::
        mh_clean

This is a simplified version of debian/rules, which shows how to use the
mh\_install and mh\_clean scripts from maven-repo-helper with CDBS and
Ant. mh\_install performs the installation of the built jars into
/usr/share/maven-repo and /usr/share/java. It uses the configuration
defined in maven-debian-helper.poms. mh\_clean does the clean up of
files left behind during the execution of mh\_install.

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

CDBS + maven-repo-helper using mh\_installpoms and mh\_installjar
-----------------------------------------------------------------

debian/control:

::

    Source: plexus-io
    Section: java
    Priority: optional
    Maintainer: Debian Java Maintainers <pkg-java-maintainers@lists.alioth.debian.org>
    Uploaders: Torsten Werner <twerner@debian.org>, Ludovic Claude <ludovic.claude@laposte.net>
    Build-Depends: debhelper (>= 5), cdbs (>= 0.4.5.3), default-jdk
    Build-Depends-Indep: maven-repo-helper (>= 1.7), maven-ant-helper (>= 7), ant, ant-optional, 
     libplexus-utils-java, libplexus-containers-java
    Standards-Version: 3.8.1
    Vcs-Svn: svn://svn.debian.org/svn/pkg-java/trunk/plexus-io
    Vcs-Browser: http://svn.debian.org/wsvn/pkg-java/trunk/plexus-io/
    Homepage: http://plexus.codehaus.org/plexus-components/plexus-io/

    Package: libplexus-io-java
    Architecture: all
    Depends: ${misc:Depends}, libplexus-utils-java, libplexus-containers-java
    Description: Plexus IO Components
     [...]

Note the Build-Depends on cdbs and maven-repo-helper.

debian/rules:

::

    #!/usr/bin/make -f
    include /usr/share/cdbs/1/rules/debhelper.mk
    include /usr/share/cdbs/1/class/ant.mk

    PACKAGE              := $(DEB_SOURCE_PACKAGE)
    VERSION              := $(DEB_UPSTREAM_VERSION)
    JAVA_HOME            := /usr/lib/jvm/default-java
    DEB_JARS             := ant-nodeps plexus-utils plexus-container-default
    DEB_ANT_BUILD_TARGET := package #javadoc
    DEB_ANT_BUILDFILE    := /usr/share/maven-ant-helper/maven-build.xml
    DEB_ANT_ARGS         := -Dbasedir=$(realpath .) -Dpackage=$(PACKAGE) -Dversion=$(VERSION)
    API_DOCS             := build/api

    binary-post-install/lib$(PACKAGE)-java::
        mh_installpoms -plib$(PACKAGE)-java
        mh_installjar -plib$(PACKAGE)-java -l pom.xml build/$(PACKAGE)-$(VERSION).jar

    clean::
        mh_clean

No patches for the POM files are needed, all is taken care of by the
mh\_installpom script (which uses mh\_cleanpom)

mh\_installpoms will install all POM files registered in
debian/lib$package-java.pom into /usr/share/maven-repo,

mh\_installjar will install the jar file associated with the POM into
/usr/share/maven-repo and also add a link to the jar in /usr/share/java,
with and without the version

debian/libplexus-io-java.pom

::

    pom.xml --no-parent

It simply lists the pom files in the source tree which need to be
installed in the repository, and indicates with the --no-parent option
that if the POM inherits from a parent POM, then this inheritance
relationship will be removed in the cleaned POM. Removing the parent
inheritence can often simplify the packaging, as the parent POM mostly
does not contain any useful information - we need only the list of
dependencies in our repository, the other details for the build are
irrelevant.

There is less configuration here because we are using the low-level
mh\_installpoms and mh\_installjar scripts which place the configuration
on their command line options.

Typical workflow for packaging a project from source
----------------------------------------------------

1. Get the original sources and unpack them, copy the debian/ folder if
   you are working on an existing package.
2. mh\_make --ant
3. edit debian/rules (to fix the last remaining packaging details)
4. edit debian/control (to add maven-repo-helper to Build-Depends: )

