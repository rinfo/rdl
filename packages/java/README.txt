########################################################################
README - Java Packages
########################################################################

Overview
========================================================================

These packages implement the rinfo content collection, supply and basic
services.

Core libraries:

* rinfo-store/README.txt
* rinfo-collector/README.txt
* rinfo-base/README.txt
* rinfo-rdf-repo/
* rinfo-sesame-http/

RInfo applications:

* rinfo-main/README.txt
* rinfo-service/README.txt

Test application:

* teststore-examples/README.txt


Setup
========================================================================

In the directory of this README, run::

    $ mvn install

If the tests fail for some reason, fix that! Still it *is* possible to do::

    $ mvn install -Dmaven.test.skip=true

if you *need* to (for some specific development/debug purpose).


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

Code coverage from tests::

    $ mvn cobertura:cobertura

Running classes::

    # .. in rinfo-base/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.base.URIMinter -Dexec.args="../../../resources/base/ <path-to-rdf-file>"

    # .. in rinfo-store/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.depot.FileDepotCmdTool -Dexec.args="<path-to-depot.properties> index"

    # .. in rinfo-store/
    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication -Dexec.args="8182 [opt. path to depot-config-props]"

