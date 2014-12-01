#!/bin/bash 

echo '------------- Bootstrap'

apt-get install sudo -y
useradd rinfo -m -G sudo -s /bin/bash

mkdir /home/rinfo/.ssh
chown rinfo:rinfo /home/rinfo/.ssh

cat id_rsa.pub >> /home/rinfo/.ssh/authorized_keys
chown rinfo:rinfo /home/rinfo/.ssh/authorized_keys

rm id_rsa.pub
