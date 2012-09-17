########################################################################
README - RInfo Main
########################################################################

Overview
========================================================================

The main web application is responsible for:

* Collecting external feeds on a regular basis;
* Perform validation of the collected documents and data against
  domain-specific rules;
* Keep a public log of the latest collects, enabling status monitoring;
* Aggregating and publish Atom archive timeline of the collected entries;
* Exposing all resources of the collected entries.

Development
========================================================================

Logging
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

In code: use slf4j API
Configured to use (declared dependency + config): log4j

Maven Usage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Testing
-------

Running specific integration tests only::

    $ mvn -Dtest=NONE -DfailIfNoTests=false -Dit.test=MainApplicationIT verify

Environments
------------

(Also see the usage of "environment" in pom.xml)::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>

Running
-------

The webapp::

    $ mvn jetty:run

Go to <http://localhost:8180/feed/current> to verify that the server was
started without errors. The page should be a valid, non-empty Atom feed if a
source (e.g. the admin feed) has been collected.

Application Design
========================================================================

Architecture
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The core web application class is ``se.lagrummet.rinfo.main.MainApplication``.
For bootstrapping, it follows the IoC pattern generally, by configuring and
assembling all components used in the system in the
``se.lagrummet.rinfo.main.Components`` class during initialization.


Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

The application has a general configuration, packaged during build time for a
specific environment.

Main configuration properties can be found in::

    src/main/resources/rinfo-main-common.properties

Specific overrides for particular environments can be found under::

    src/environments/

Runtime Configuration
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

A special "admin feed" is expected to contain recognized entry ids, defined in
the general configuration. Such entries contain (RDF) data which descibes the
initial domain data. These are read by StorageHandler:s, configured in
Components.

In particular, rinfo-main needs access to an admin feed. Follow the
instructions in <../../../manage/running_rinfo_locally.txt> for how to set up
and serve such a feed.

Manually pinging rinfo-main to collect from a test source::

    $ curl --data "feed=http://localhost:8280/feed/current" http://localhost:8180/collector

Collection and Storage
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

Feed sources are discovered using the configuration mechanisms described above.
The FeedCollectScheduler runs a FeedCollector at defined intervals. The
FeedCollector writes to a Depot, which implements an Entry storage and achive
feed. StorageHandler components intercepts certain entries for metadata
configuration and validation purposes.

