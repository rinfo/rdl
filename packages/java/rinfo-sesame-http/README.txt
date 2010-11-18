########################################################################
README - RInfo Sesame HTTP
########################################################################

Right now this module is just a way to fetch the Sesame binaries. After 
running mvn clean package you will find two war files in 
target/dependency

The module may develop into a wrapper of Sesame at a later point.

You can also run openrdf-sesame with the command "mvn jetty:run-war". 
This is useful when testing rinfo-service as it depends on a sesame 
instance being up and running.