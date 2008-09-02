########################################################################
README - RInfo Store
########################################################################

Maven Usage
========================================================================

Testing::

    # All:
    $ mvn clean test
    # One:
    $ mvn test -Dtest=se.lagrummet.rinfo.store.depot.FileDepotWriteTest
    # By pattern matching:
    $ mvn test -Dtest=FileDepot*Test

Running a class::

    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication -Dexec.args="8182 [opt. path to depot-config-props]"


