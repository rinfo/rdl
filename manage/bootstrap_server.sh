# Set up remote server installation 
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.

apt-get update
apt-get install git -y
apt-get install build-essential python-dev python-pkg-resources python-setuptools sudo -y
easy_install pip
pip install fabric

ssh root@$1 apt-get install sudo -y 
ssh root@$1 useradd rinfo -m -G sudo -s /bin/bash
ssh root@$1 passwd rinfo 

mkdir /tmp/install /tmp/install/rinfo
cd /tmp/install/rinfo
git clone https://github.com/rinfo/rdl.git
cd rdl
if [ -z "$2" ]; then
	git checkout develop
else
	git checkout $2
fi
cd manage/fabfile
if [ -z "$3" ]; then
	fab target.integration sysconf.install_server:hosts=$1
else
	fab target.$3 sysconf.install_server:hosts=$1
fi



