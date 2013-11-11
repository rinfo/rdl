# Prepare local install machine for running Fabric scripts
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.

apt-get update
apt-get install git -y
apt-get install build-essential python-dev python-pkg-resources python-setuptools sudo -y
easy_install pip
pip install fabric

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

#fab target.testfeed -R demosource sysconf.install_server sysconf.config_server app.demodata.deploy_testfeed server.reload_apache


