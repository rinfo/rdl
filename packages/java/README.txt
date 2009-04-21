########################################################################
README - Java Packages
########################################################################

Setup
========================================================================

In the directory of this README, run::

    $ mvn install

Environments
========================================================================

Study how the use of ``${environment}`` in the ``pom.xml`` is used to copy
environment-specific resources to the target for packaging. This should *not*
be used by shared library packages, only final applications!

Basic usage within application sub-packages::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>

Maven Usage
========================================================================

Testing::

    # .. in <some-package>
    # All:
    $ mvn clean test
    # One:
    $ mvn test -Dtest=se.lagrummet.rinfo.store.depot.FileDepotWriteTest
    # By pattern matching:
    $ mvn test -Dtest=FileDepot*Test

Running classes::

    # .. in rinfo-base/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.base.URIMinter -Dexec.args="../../../resources/base/ <path-to-rdf-file>"

    # .. in rinfo-store/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.depot.FileDepotCmdTool -Dexec.args="<path-to-depot.properties> index"

    # .. in rinfo-store/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication -Dexec.args="8182 [opt. path to depot-config-props]"

