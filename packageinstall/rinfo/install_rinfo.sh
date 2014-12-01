#!/bin/bash

echo '------------- Install Rinfo-Main'

mv rinfo-main /etc/apache2/sites-available/rinfo-main
chown root:root /etc/apache2/sites-available/rinfo-main
chmod 644 /etc/apache2/sites-available/rinfo-main

a2ensite rinfo-main

mv rinfo-main.war /var/lib/tomcat7/webapps/
