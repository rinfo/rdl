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

, or::

    $ mvn -Djetty.port=8180 jetty:run

Manually pinging rinfo-main to collect from a test source::

    $ curl --data "feed=http://localhost:8280/admin/feed/current" http://localhost:8180/collector

