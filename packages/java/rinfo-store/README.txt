########################################################################
README - RInfo Core
########################################################################

Common Maven Uses
========================================================================

Environments (see usage of "environment" in pom.xml)::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>

Testing::

    # All:
    $ mvn clean test
    # One:
    $ mvn test -Dtest=se.lagrummet.rinfo.store.depot.FileDepotWriteTest
    # By pattern matching:
    $ mvn test -Dtest=FileDepot*Test

Running a class::

    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication

Running a war project::

    $ mvn -Djetty.port=8180 jetty:run


Running Groovy outside of maven with declared dependencies
========================================================================

To put dependencies in classpath, use (see inside for details)::

    $ . src/scripts/setclasspath.sh -b

To use a named environment, use (see environments in pom.xml)::

    $ . src/scripts/setclasspath.sh prod

Then run with:

    $ groovy <path-to-script>

This works for groovy classes, including tests, as well as plain scripts (see "src/scripts/*.groovy").


