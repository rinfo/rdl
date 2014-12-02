#!/bin/bash

echo '------------- Install Rinfo-Service'

mv service /etc/apache2/sites-available/service
chown root:root /etc/apache2/sites-available/service
chmod 644 /etc/apache2/sites-available/service

a2ensite service

mv rinfo-service.war /var/lib/tomcat7/webapps/

chown root:rinfo /usr/share/tomcat7

mkdir -p /usr/share/tomcat7/logs
chmod 777 /usr/share/tomcat7/logs
chown root: /usr/share/tomcat7/logs/

mkdir -p /usr/share/tomcat7/.aduna
chmod 777 /usr/share/tomcat7/.aduna
chown root: /usr/share/tomcat7/.aduna

echo 'info.aduna.platform.appdata.basedir=/opt/rinfo/sesame-repo' >> /var/lib/tomcat7/conf/catalina.properties
