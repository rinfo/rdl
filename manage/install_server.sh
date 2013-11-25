# Prepare local install machine for running Fabric scripts
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.
# Will create subdirectory rinfo and checkout develop branch or selected version/feature

rm -rf /tmp/install
mkdir /tmp/install /tmp/install/rinfo
cd /tmp/install/rinfo
git clone https://github.com/rinfo/rdl.git
cd rdl
if [ -z "$1" ]; then
	git checkout develop
else
	git checkout $1
fi

cd manage/fabfile

echo "Enter sudo password: "
read pwd

fab -p $pwd target.$2 -R main,service,checker,admin sysconf.install_server sysconf.configure_server
fab -p $pwd target.$2 -R main,service,checker,admin app.service.install_elasticsearch
fab -p $pwd target.$2 -R main,service,checker,admin app.service.start_elasticsearch
fab -p $pwd target.$2 -R main,service,checker,admin app.service.deploy_sesame
fab -p $pwd target.$2 -R admin app.admin.all
fab -p $pwd target.$2 -R main app.main.all
fab -p $pwd target.$2 -R service app.service.all
fab -p $pwd target.$2 -R checker app.checker.all
#fab -p $pwd target.$2 -R server.restart_all
fab -p $pwd target.$2 -R admin app.admin.ping_main
#fab -p $pwd app.docs.build
#fab -p $pwd target.$2 app.docs.deploy
fab -p $pwd target.$2 -Rmain sysconf.sync_static_web






