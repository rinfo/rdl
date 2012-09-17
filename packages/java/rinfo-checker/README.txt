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

Running webapp::

    $ mvn jetty:run

Running standalone tool::

    $ mvn exec:java -Pdev-unix -Dcatalina.home=target/fake-catalina-home -Dexec.mainClass=se.lagrummet.rinfo.checker.CheckerTool -Dexec.args="FEED_URL OPT_MAX_ENTRIES"

