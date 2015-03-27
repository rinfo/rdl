#!/bin/bash 

echo '------------- Install Tomcat Cent OS'

yum -y install java-1.7.0-openjdk

yum -y install tomcat 

service tomcat stop
rm -rf /var/lib/tomcat/webapps/*

mkdir -p /opt/tomcat/logs
chown tomcat /opt/tomcat/logs
