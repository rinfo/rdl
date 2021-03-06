#!/bin/sh
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
fab target.$2 -R main,service,checker,admin app.service.install_varnish
fab target.$2 -R main,service,checker,admin app.service.start_varnish
fab target.$2 -R main,service,checker,admin app.service.deploy_sesame
fab target.$2 -R admin app.admin.all
fab target.$2 -R main app.main.all
fab target.$2 -R service app.service.all
fab target.$2 -R checker app.checker.all

read -p "[press any key; to restart tomcat and apache on target server(s)]" -s -n1
fab target.$2 -R main server.restart_all

#TODO: make sure that collect of admin-feed is done after lagrummet is installed...
#...to avoid that any scheduled collect of sources is interrupted by apache/tomcat restarts
#read -p "[press any key; to start collection via ping_main]" -s -n1
#fab target.$2 -R admin app.admin.ping_main