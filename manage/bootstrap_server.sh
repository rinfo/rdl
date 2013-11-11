# Prepare remote server for fabric installation 
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.

# Install rinfo user and sudo
ssh root@$1 apt-get install sudo -y 
ssh root@$1 useradd rinfo -m -G sudo -s /bin/bash
ssh root@$1 passwd rinfo 

# Automate login
ssh root@$1 mkdir /home/rinfo/.ssh
scp ~/.ssh/id_rsa.pub root@$1:/home/rinfo/.
ssh root@$1 cat /home/rinfo/id_rsa.pub >> /home/rinfo/.ssh/authorized_keys
ssh root@$1 rm /home/rinfo/id_rsa.pub


