########################################################################
README - Java Packages
########################################################################


Tips for Running Groovy outside of Maven
========================================================================

Do the following to use the declared dependencies in a pom.xml from groovy
without running it via Maven. (This makes for a speedier roundtrip during
testing etc.)

1. Go to the package root directory (where the pom.xml is).

2. Put dependencies in classpath with (see inside for details)::

    $ source ../setclasspath.sh -b

3. To use a named environment, use (see environments in pom.xml)::

    $ source ../setclasspath.sh prod

4. Then run with::

    $ groovy <path-to-script>

This works for groovy classes, including tests, as well as plain scripts (e.g.
"src/scripts/*.groovy").


