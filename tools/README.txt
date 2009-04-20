########################################################################
README - Tools
########################################################################

Introduction
========================================================================

These tools are for instrumental (development) use of the rinfo packages.

Running Groovy outside of Maven
========================================================================

(This makes for a speedier roundtrip when using groovy for testing and
prototyping.)

You can use the ``classpathjar.groovy`` script to create a jar file containing
manifest references to a maven2 package and its dependencies. Put this on the
classpath (e.g. $CLASSPATH, using the ``-cp`` param to the groovy exe, or put
it in ``~/.groovy/lib/``).

Example:

1. Create a jar referencing dependencies in rinfo-service::

    $ groovy classpathjar.groovy ../ packages/java/rinfo-service/ ~/.groovy/lib/rinfoclasspath.jar

2. Run groovy as usual with::

    $ groovy <path-to-script>

