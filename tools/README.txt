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

