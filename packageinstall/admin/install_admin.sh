#!/bin/bash 

echo '------------- Install Rinfo-Admin CentOS'

sed 's/dnsplaceholderforsed/'$1'/g' admin > tmp_admin
sed 's/dnsplaceholderforsed/lagrummet\.se/g' admin >> tmp_admin

#mkdir -p /etc/httpd/sites-available 
#mkdir -p /etc/httpd/sites-enabled

mv tmp_admin /etc/httpd/conf.d/admin.conf
#mv tmp_admin /etc/httpd/sites-available/admin.conf
#ln -s /etc/httpd/sites-available/admin.conf /etc/httpd/sites-enabled/admin.conf

#chown root:root /etc/httpd/sites-available/admin.conf
#chmod 644 /etc/httpd/sites-available/admin.conf

mkdir -p /var/www/admin
( cd output && cp -r * /var/www/admin/ )
chmod -R 755 /var/www/admin
chown -R rinfo:root /var/www/admin

chown -R rinfo:rinfo /var/www/admin/*
