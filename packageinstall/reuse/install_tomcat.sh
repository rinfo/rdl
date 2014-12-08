#!/bin/bash 

echo '------------- Install Tomcat'

apt-get install openjdk-7-jre-headless -y

apt-get install tomcat7 -y
/etc/init.d/tomcat7 stop
rm -rf /var/lib/tomcat7/webapps/*

chmod 0755 /etc/init.d/tomcat7
update-rc.d tomcat7 defaults

mkdir -p /opt/tomcat/logs
chown rinfo /opt/tomcat/logs
