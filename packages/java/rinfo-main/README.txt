########################################################################
README - RInfo Main
########################################################################

Maven Usage
========================================================================

Environments (see usage of "environment" in pom.xml)::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>


Specific testing::
    # ...


Running as war::

    $ mvn -Djetty.port=8180 jetty:run


Running Groovy outside of maven with declared dependencies
========================================================================

To run the test/setup scripts in <file:src/scripts>.

Put dependencies in classpath with (see inside for details)::

    $ . src/scripts/setclasspath.sh -b

To use a named environment, use (see environments in pom.xml)::

    $ . src/scripts/setclasspath.sh prod

Then run with:

    $ groovy <path-to-script>

This works for groovy classes, including tests, as well as plain scripts (see "src/scripts/*.groovy").


