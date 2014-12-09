#!/bin/bash

echo '------------- Install Rinfo-Checker'

mv checker /etc/apache2/sites-available/checker
chown root:root /etc/apache2/sites-available/checker
chmod 644 /etc/apache2/sites-available/checker

a2ensite checker

mv rinfo-checker.war /var/lib/tomcat7/webapps/
