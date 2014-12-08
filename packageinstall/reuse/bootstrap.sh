#!/bin/bash 

echo '------------- Bootstrap'

apt-get install sudo -y
useradd rinfo -m -G sudo -s /bin/bash

mkdir /home/rinfo/.ssh
chown rinfo:rinfo /home/rinfo/.ssh

cat id_rsa.pub >> /home/rinfo/.ssh/authorized_keys
chown rinfo:rinfo /home/rinfo/.ssh/authorized_keys

sed -i 's/^#PermitRootLogin yes/PermitRootLogin no/;s/PermitRootLogin yes/PermitRootLogin no/;s/^#PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/^#X11Forwarding yes/X11Forwarding no/;s/X11Forwarding yes/X11Forwarding no/' /etc/ssh/sshd_config

rm id_rsa.pub
