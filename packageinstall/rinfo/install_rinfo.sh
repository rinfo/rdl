#!/bin/bash

echo '------------- Install Rinfo-Main CentOS'

sed 's/dnsplaceholderforsed/'$1'/g' rinfo-main > tmp_rinfo-main
sed 's/dnsplaceholderforsed/lagrummet\.se/g' rinfo-main >> tmp_rinfo-main

cp /etc/httpd/conf/httpd.conf .
cat tmp_rinfo-main >> httpd.conf 
sudo cp httpd.conf /etc/httpd/conf/httpd.conf 

cp rinfo-main.war /var/lib/tomcat/webapps/

mkdir -p /opt/rinfo/store
chown -R tomcat /opt/rinfo/store

sed 's/dnsplaceholderforsed/'$1'/g' rinfo-main.properties > tmp_rinfo-main.properties
mkdir -p /etc/rinfo
cp tmp_rinfo-main.properties /etc/rinfo/rinfo-main.properties