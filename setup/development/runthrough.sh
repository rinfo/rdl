#!/bin/bash

# Prerequisites:
#   - /opt/_workapps/rinfo/depots/example.org/
#   - /opt/_workapps/rinfo/depots/rinfo/ => EMPTY!
#   - http://localhost:8080/openrdf-sesame/

#========
echo "Start example depot supply"
(
    #pushd teststore-examples/
    #mvn -Djetty.port=8182 jetty:run
    # or..
    #mvn exec:java -Dexec.mainClass=se.lagrummet.rinfo.store.supply.SupplyApplication -Dexec.args="8182" # 2>&1 > /dev/null &
    # popd
)
##========
echo "Collect example data + base data; run Supply"
#(
#    source src/scripts/setclasspath.sh dev-unix
#    groovy src/scripts/collect_feed.groovy http://localhost:8182/feed/current
#    groovy src/scripts/base_to_depot.groovy
#    groovy src/scripts/run_supply.groovy 8180 2>&1 > /dev/null &
#)
##========
echo "Feed To Sesame"
#(
#    source src/scripts/setclasspath.sh dev-unix
#    groovy src/scripts/feed_to_sesame.groovy http://localhost:8180/feed/current http://localhost:8080/openrdf-sesame/ SYSTEM
#
#)
##========
#
