#!/bin/bash 

echo '------------- Install Rinfo-Admin'

mv admin /etc/apache2/sites-available/admin
chown root:root /etc/apache2/sites-available/admin
chmod 644 /etc/apache2/sites-available/admin

mkdir -p /var/www/admin
( cd output && cp -r * /var/www/admin/ )
chmod -R 755 /var/www/admin
chown -R rinfo:root /var/www/admin

chown -R rinfo:rinfo /var/www/admin/*

a2ensite admin
