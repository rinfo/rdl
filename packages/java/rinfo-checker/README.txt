########################################################################
README - RInfo Checker
########################################################################

Overview
========================================================================

A utility application intended as a tool for RInfo source providers (i.e.
involved agencies) to verify their source feeds and its payload (primarily the
RDF).

Basically runs the same collector and verification mechanisms as RInfo Main.

Usage
========================================================================

Running standalone::

    $ mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.checker.restlet.CheckerApplication -Dexec.args="8182 src/main/webapp/media/""

