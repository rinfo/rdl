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

To run a service tool, e.g. reindexing an elasticsearch index from a running
sesame in a local experiment, invoke::

    $ mvn exec:java -Dexec.mainClass=rinfo.service.cmd.GenElastic \
        -Dexec.args="src/environments/local/rinfo-service.properties /opt/work/rinfo/bak/elastic_text_extracts"


Configuration & Runtime
========================================================================

In development mode, rinfo-main doesn't ping rinfo-service after collection.

To ping manually (telling rinfo-service to collect from rinfo-main)::

    $ curl --data "feed=http://localhost:8180/feed/current" \
      http://localhost:8181/collector

