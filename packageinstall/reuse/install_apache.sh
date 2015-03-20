#!/bin/bash 

echo '------------- Install Apache CentOS'

yum -y install httpd

apachectl stop

mv robots.txt /var/www/
chmod u=rw,a=r /var/www/robots.txt
chown root:root /var/www/robots.txt

sudo rm -rf /etc/httpd/conf.d/*
