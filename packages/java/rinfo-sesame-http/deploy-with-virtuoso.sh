#!/bin/bash

TOMCAT_HOME=/opt/tomcat
SESAME_WORKBENCH=$TOMCAT_HOME/webapps/sesame-workbench

mvn package
cp target/dependency/openrdf-sesame.war $TOMCAT_HOME/webapps
cp target/dependency/sesame-workbench.war $TOMCAT_HOME/webapps

#/opt/tomcat/bin/startup.sh
#/opt/tomcat/bin/shutdown.sh

# Files either from local Virtuoso install/source, or <http://virtuoso.openlinksw.com/dataspace/dav/wiki/Main/VOSDownload>
cp /opt/local/lib/sesame/virt_sesame2.jar $TOMCAT_HOME/webapps/openrdf-sesame/WEB-INF/lib/
cp /opt/local/lib/jdbc-3.0/virtjdbc3.jar $TOMCAT_HOME/webapps/openrdf-sesame/WEB-INF/lib/

cp /opt/local/lib/sesame/virt_sesame2.jar $SESAME_WORKBENCH/WEB-INF/lib/
cp /opt/local/lib/jdbc-3.0/virtjdbc3.jar $SESAME_WORKBENCH/WEB-INF/lib/

mv $SESAME_WORKBENCH/transformations/create.xsl $SESAME_WORKBENCH/transformations/create.xsl-original
cp /opt/local/lib/sesame/create.xsl $SESAME_WORKBENCH/transformations/
cp /opt/local/lib/sesame/create-virtuoso.xsl $SESAME_WORKBENCH/transformations/

# Make Virtuoso is started. How to start in foreground with default config:
#virtuoso-t -c /opt/local/var/lib/virtuoso/db/virtuoso.ini -f

#/opt/tomcat/bin/startup.sh

