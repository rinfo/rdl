#!/bin/bash

# Prerequisites:
#   - /opt/_workapps/rinfo/depots/example.org/
#   - /opt/_workapps/rinfo/depots/rinfo/ => EMPTY!
#   - http://localhost:8080/openrdf-sesame/

#========
echo "Start example depot supply"
(
    source src/scripts/setclasspath.sh dev-local/example
    groovy src/scripts/run_supply.groovy 2>&1 > /dev/null &
)
##========
#echo "Collect example data + base data; run Supply"
#(
#    source src/scripts/setclasspath.sh dev-unix
#    groovy src/scripts/collect_feed.groovy http://localhost:8182/feed/current
#    groovy src/scripts/base_to_depot.groovy
#    groovy src/scripts/run_supply.groovy 8180 2>&1 > /dev/null &
#)
##========
#echo "Feed To Sesame"
#(
#    source src/scripts/setclasspath.sh dev-unix
#    groovy src/scripts/feed_to_sesame.groovy http://localhost:8180/feed/current http://localhost:8080/openrdf-sesame/ SYSTEM
#
#)
##========
#
