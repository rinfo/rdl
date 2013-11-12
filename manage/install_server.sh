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

fab target.$2 -R main,service,checker,admin sysconf.install_server sysconf.configure_server
fab target.$2 -R main,service,checker,admin app.service.install_elasticsearch
fab target.$2 -R main,service,checker,admin app.service.start_elasticsearch
fab target.$2 -R main,service,checker,admin app.service.deploy_sesame
fab target.$2 -R main,service,checker,admin app.main.all app.checker.all app.admin.all app.service.all
fab target.$2 -R main,service,checker,admin app.admin.ping_main
#fab app.docs.build
#fab target.$2 app.docs.deploy
fab target.$2 -Rmain sysconf.sync_static_web






