#!/bin/sh
# Prepare local install machine for running Fabric scripts
# Copy the contents of this script into a shell script file and remember to make it executable. Then run it.
# Will create subdirectory rinfo and checkout develop branch or selected version/feature

sudo apt-get update
sudo apt-get install git -y
sudo apt-get install build-essential python-dev python-pkg-resources python-setuptools sudo -y
sudo easy_install pip
sudo pip install fabric

mkdir rinfo
cd rinfo
git clone https://github.com/rinfo/rdl.git
cd rdl
if [ -z "$1" ]; then
	git checkout develop
else
	git checkout $1
fi




