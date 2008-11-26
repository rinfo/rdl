########################################################################
README - Example Stores
########################################################################

Maven Usage
========================================================================

Environments (see usage of "environment" in pom.xml)::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>


Running as war::

    $ mvn -Djetty.port=8182 jetty:run


