apt-get update

apt-get install sudo -y
useradd rinfo -m -G sudo -s /bin/bash
passwd rinfo

cat id_rsa.pub >> .ssh/authorized_keys
rm id_rsa.pub

apt-get install apache2 -y
/usr/sbin/a2enmod proxy_ajp
/usr/sbin/a2enmod proxy_http
apt-get install openjdk-7-jre-headless -y

apt-get install tomcat7 -y
/etc/init.d/tomcat stop
rm -rf /var/lib/tomcat7/webapps/*

mv init_d.tomcat /etc/init.d/tomcat
chmod 0755 /etc/init.d/tomcat
update-rc.d tomcat defaults
sed -i 's/^#PermitRootLogin yes/PermitRootLogin no/;s/PermitRootLogin yes/PermitRootLogin no/;s/^#PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/^#X11Forwarding yes/X11Forwarding no/;s/X11Forwarding yes/X11Forwarding no/' /etc/ssh/sshd_config
/etc/init.d/ssh restart
mv robots.txt /var/www/
chmod u=rw,a=r /var/www/robots.txt

mv workers.properties /etc/apache2/

mv jk.conf /etc/apache2/conf.d/

mv rinfo-main > /etc/apache2/sites-available/rinfo-main

a2ensite rinfo-main

mkdir -p /opt/tomcat
chown rinfo /opt/tomcat
mkdir -p /opt/rinfo
chown rinfo /opt/rinfo/

mv rinfo-main.war /var/lib/tomcat7/webapps/

mkdir -p /opt/tomcat/logs
chown rinfo /opt/tomcat/logs

/etc/init.d/tomcat start
/etc/init.d/apache2 start
