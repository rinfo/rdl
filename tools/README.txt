########################################################################
README - Tools
########################################################################

Introduction
========================================================================

These tools are for instrumental (development) use of the rinfo packages.


Running Groovy outside of Maven
========================================================================

Option A: Use Groovy's Grape With Local Maven Repo
------------------------------------------------------------------------

Groovy's dependency mechanism (using ``@Grab``) annotations can be used with
your local Maven repository. To do this, locate the file:

    <$HOME/.groovy/grapeConfig.xml>

If it doesn't exist, see:

    <http://groovy.codehaus.org/Grape>

for how to create a default version.

Then add, directly after ``ivysettings/settings``, the following directive::

    <caches useOrigin="true"/>

And, in ``/ivysettings/resolvers/chain``, after the first ``filesystem``, add::

    <filesystem name="local-maven2" m2compatible="true">
        <ivy pattern="${user.home}/.m2/repository/[organisation]/[module]/[revision]/[module]-[revision].pom"/>
        <artifact pattern="${user.home}/.m2/repository/[organisation]/[module]/[revision]/[artifact]-[revision](-[classifier]).[ext]"/>
    </filesystem>

The path in the root property above should match the location for your local
maven repository. You may have changed that from the default home directory.

The current stable version of Ivy (2.1.0), the dependency manager that
Groovy uses, has problems parsing some of the dependency information
contained in the Sesame packages (org.openrdf.* and
info.aduna.*). However, the current development version of Ivy has
support for this. This version can be installed with the following commands:

$ cd $GROOVY_HOME/lib
$ mv ivy-2.1.0.jar ivy-2.1.0.jar-old
$ wget http://hudson.zones.apache.org/hudson/view/Ant/job/Ivy/lastSuccessfulBuild/artifact/trunk/build/artifact/jars/ivy.jar

That should make all Maven dependencies locatable (provided that you have ``mvn
install``:ed your packages; see <packages/java/README.txt>).


Option B: Use A Pathing Jar
------------------------------------------------------------------------

(This makes for a speedier roundtrip when using groovy for testing and
prototyping.)

You can use the ``classpathjar.groovy`` script to create a jar file containing
manifest references to a maven2 package and its dependencies. Put this on the
classpath (e.g. $CLASSPATH, using the ``-cp`` param to the groovy exe, or put
it in ``~/.groovy/lib/``).

Before running the classpathjar.groovy script make sure you run ``mvn install``
in the packages/java folder.

Example:

1. Create a jar referencing dependencies in rinfo-service::

    $ groovy classpathjar.groovy ../packages/java/rinfo-service/ ~/.groovy/lib/rinfoclasspath.jar

2. Run groovy as usual with::

    $ groovy <path-to-script>


Building the PDF documents
========================================================================
To build the PDF documents, use the build_rinfo_docs.groovy script
(This can also be done using the fabric command "build_docs", see
../manage/README.txt). 

The stylesheets used for PDF generation requires that font files for
Garamond and Trebuchet MS are present. The needed files are
"gara.ttf", "garait.ttf", "garabd.ttf", "trebuc.ttf", "trebucit.ttf",
"trebucbi.ttf" and "trebucbd.ttf". Place these files in the same directory as the build_rinfo_docs.groovy script.
