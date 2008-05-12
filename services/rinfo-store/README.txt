########################################################################
README - RInfo Core
########################################################################

Common Maven Uses
========================================================================

Testing::

    # All:
    $ mvn clean test
    # One:
    $ mvn test -Dtest=se.lagrummet.rinfo.store.depot.FileDepotWriteTest
    # By pattern matching:
    $ mvn test -Dtest=FileDepot*Test

Running a class::

    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication


Running Groovy outside of maven with declared dependencies
========================================================================

To put dependencies in classpath, use (see inside for details)::

    $ . setclasspath.sh -b

Then run with:

    $ groovy <path-to-script>

