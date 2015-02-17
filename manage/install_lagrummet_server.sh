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

fab target.$2 -R lagrummet sysconf.install_server sysconf.configure_server
fab target.$2 -R lagrummet sysconf.sync_static_web






