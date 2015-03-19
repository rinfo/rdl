#!/bin/bash 

echo '------------- Install Apache CentOS'

yum -y install httpd

apachectl stop

echo 'LoadModule proxy_ajp_module modules/mod_proxy_ajp.so' >> /etc/httpd/conf/httpd.conf
echo 'LoadModule proxy_http_module modules/mod_proxy_http.so' >> /etc/httpd/conf/httpd.conf
echo 'Include conf.modules.d/*.conf' >> /etc/httpd/conf/httpd.conf
#echo 'IncludeOptional sites-enabled/*.conf' >> /etc/httpd/conf/httpd.conf

#sed -i s/#OPTIONS=/OPTIONS=-k/g /etc/sysconfig/httpd

mv robots.txt /var/www/
chmod u=rw,a=r /var/www/robots.txt
chown root:root /var/www/robots.txt

#mv jk.conf /etc/apache2/conf.d/

