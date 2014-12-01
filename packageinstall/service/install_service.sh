#!/bin/bash

echo '------------- Install Rinfo-Service'

mv service /etc/apache2/sites-available/service
chown root:root /etc/apache2/sites-available/service
chmod 644 /etc/apache2/sites-available/service

a2ensite service

mv rinfo-service.war /var/lib/tomcat7/webapps/

