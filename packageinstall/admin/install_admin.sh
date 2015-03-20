#!/bin/bash 

echo '------------- Install Rinfo-Admin CentOS'

sed 's/dnsplaceholderforsed/'$1'/g' admin > tmp_admin
sed 's/dnsplaceholderforsed/lagrummet\.se/g' admin >> tmp_admin

cp /etc/httpd/conf/httpd.conf .
cat tmp_admin >> httpd.conf 
sudo cp httpd.conf /etc/httpd/conf/httpd.conf 

mkdir -p /var/www/admin
( cd output && cp -r * /var/www/admin/ )
chmod -R 755 /var/www/admin
chown -R rinfo:root /var/www/admin

chown -R rinfo:rinfo /var/www/admin/*

echo '127.0.0.1  admin.'$1 >> /etc/hosts
echo '127.0.0.1  admin.lagrummet.se' >> /etc/hosts
