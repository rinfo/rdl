#!/bin/sh
# Prepare remote server for fabric installation
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.

# Install rinfo user and sudo
ssh root@$1 apt-get install sudo -y 
ssh root@$1 useradd rinfo -m -G sudo -s /bin/bash
ssh root@$1 passwd rinfo

# Prefere ipv4, to speed up debian updates
ssh root@$1 'echo "precedence ::ffff:0:0/96  100" >>  /etc/gai.conf'

# Automate login
ssh rinfo@$1 mkdir .ssh
scp ~/.ssh/id_rsa.pub rinfo@$1:.
ssh rinfo@$1 'cat id_rsa.pub >> .ssh/authorized_keys'
ssh rinfo@$1 rm id_rsa.pub



