########################################################################
README - Tools
########################################################################

Introduction
========================================================================

These tools are for instrumental (development) use of the rinfo packages.

Running Groovy outside of Maven
========================================================================

You can use the ``setclasspath.sh`` to define a classpath based on dependencies
declared in a pom.xml. (This makes for a speedier roundtrip when using groovy
for testing and prototyping.)

1.

    A. Create a classpath.txt with the dependencies and use it with::

        $ . setclasspath.sh <PATH_TO_POM_DIR>

    B. Or use an existing classpath.txt by::

        $ . setclasspath.sh

2. Run groovy as usual with::

    $ groovy <path-to-script>

