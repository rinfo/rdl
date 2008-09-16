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


