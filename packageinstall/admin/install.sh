apt-get update

apt-get install sudo -y
useradd rinfo -m -G sudo -s /bin/bash
passwd rinfo

cat id_rsa.pub >> .ssh/authorized_keys
rm id_rsa.pub

apt-get install apache2 -y
/usr/sbin/a2enmod proxy_ajp
/usr/sbin/a2enmod proxy_http

/etc/init.d/ssh restart
mv robots.txt /var/www/
chmod u=rw,a=r /var/www/robots.txt
chown root:root /var/www/robots.txt

mv jk.conf /etc/apache2/conf.d/

mv admin /etc/apache2/sites-available/admin
chown root:root /etc/apache2/sites-available/admin
chmod 644 /etc/apache2/sites-available/admin

mkdir -p /var/www/admin
( cd output && cp -r * /var/www/admin/ )
chmod -R 755 /var/www/admin
chown -R rinfo:root /var/www/admin

chown -R rinfo:rinfo /var/www/admin/*

a2ensite admin

/etc/init.d/apache2 restart
