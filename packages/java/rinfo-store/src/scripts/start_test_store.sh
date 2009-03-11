#!/bin/bash
java se.lagrummet.rinfo.store.supply.SupplyApplication 8182 src/test/resources/rinfo-depot.properties
# TODO: The test properties refers to feedSkeleton which is only in (class)path during test phase.
#mvn -e exec:java -Dexec.mainClass="se.lagrummet.rinfo.store.supply.SupplyApplication" -Dexec.args="8182 src/test/resources/rinfo-depot.properties"
