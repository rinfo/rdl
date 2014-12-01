#!/bin/bash 

echo '------------- Install Tomcat'

apt-get install openjdk-7-jre-headless -y

apt-get install tomcat7 -y
/etc/init.d/tomcat stop
rm -rf /var/lib/tomcat7/webapps/*

mv init.d_tomcat /etc/init.d/tomcat7
chmod 0755 /etc/init.d/tomcat7
update-rc.d tomcat7 defaults
sed -i 's/^#PermitRootLogin yes/PermitRootLogin no/;s/PermitRootLogin yes/PermitRootLogin no/;s/^#PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/^#X11Forwarding yes/X11Forwarding no/;s/X11Forwarding yes/X11Forwarding no/' /etc/ssh/sshd_config

mkdir -p /opt/tomcat/logs
chown rinfo /opt/tomcat/logs
