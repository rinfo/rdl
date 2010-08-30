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

Environments (see usage of "environment" in pom.xml)::

    # Use inferred environment (default or e.g. by OS):
    $ mvn <...>
    # Use config in src/environments/prod/:
    $ mvn -Pprod <...>

Running as war::

    $ mvn clean compile war:war jetty:run-war -Djetty.port=8180 -Dmaven.test.skip=true

, or (TODO: doesn't work yet)::

    $ mvn -Djetty.port=8180 jetty:run

Goto http://localhost:8180/feed/current to verify that the server was started without errors. The page should be a valid, none empty Atom feed.

Main configuration properties can be found in src/main/resources/rinfo-main/src/main/resources/rinf-main-common.properties. Specific overrides for particular environments can be found under src/environments/. In particular, rinfo-main needs access to an admin feed. Follow the instructions in ../../../manage/running_rinfo_locally.txt to set up and serve such a feed.

Manually pinging rinfo-main to collect from the test source at http://localhost:8280/feed/current::

    $ curl --data "feed=http://localhost:8280/feed/current" http://localhost:8180/collector

