########################################################################
README - RInfo Service
########################################################################

Overview
========================================================================

This service indexes the main data feed as RDF in a triplestore, and expose
some services for that.

Maven Usage
========================================================================

Running the webapp::

    $ mvn jetty:run

Go to <http://localhost:8181/rdata/org> to verify that the server was 
started OK. Note that you need to run a Sesame database for service to work.

Configuration & Runtime
========================================================================

In development mode, rinfo-main doesn't ping rinfo-service after collection.

To ping manually (telling rinfo-service to collect from rinfo-main)::

    $ curl --data "feed=http://localhost:8180/feed/current" \
      http://localhost:8181/collector

