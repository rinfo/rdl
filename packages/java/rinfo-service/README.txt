########################################################################
README - RInfo Service
########################################################################

Overview
========================================================================

This service indexes the main data feed as RDF in a triplestore, and expose
some services for that.

Running as war::

    $ mvn clean compile war:war jetty:run-war -Djetty.port=8181 -Dmaven.test.skip=true

In development mode, rinfo-main doesn't ping rinfo-service after collection. To
do this manually, call e.g.::

Manually pinging rinfo-service to collect from rinfo-main::

    $ curl --data "feed=http://localhost:8180/feed/current" http://localhost:8181/collector

