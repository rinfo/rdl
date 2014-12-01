#!/bin/bash 

echo '------------- Install Apache'

apt-get install apache2 -y
/usr/sbin/a2enmod proxy_ajp
/usr/sbin/a2enmod proxy_http

/etc/init.d/ssh restart
mv robots.txt /var/www/
chmod u=rw,a=r /var/www/robots.txt
chown root:root /var/www/robots.txt

mv jk.conf /etc/apache2/conf.d/

/etc/init.d/apache2 stop
