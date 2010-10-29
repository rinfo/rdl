########################################################################
README - RInfo Main
########################################################################

Overview
========================================================================

The main web application exposes all collected resources, along with an
Atom archive timeline.

Logging
========================================================================

In code: use slf4j API
Configured to use (declared dependency + config): log4j

Maven Usage
========================================================================

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

Configuration & Runtime
========================================================================

Main configuration properties can be found in::

    src/main/resources/rinfo-main-common.properties

Specific overrides for particular environments can be found under::

    src/environments/

In particular, rinfo-main needs access to an admin feed. Follow the
instructions in <../../../manage/running_rinfo_locally.txt> for how to set up
and serve such a feed.

Manually pinging rinfo-main to collect from a test source::

    $ curl --data "feed=http://localhost:8280/feed/current" http://localhost:8180/collector

