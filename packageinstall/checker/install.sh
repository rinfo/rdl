#!/bin/bash

echo '------------- Install Rinfo-checker multi node'

apt-get update
source ./bootstrap.sh
source ./install_apache.sh
source ./install_tomcat.sh

source ./create_folders.sh
source ./install_checker.sh

source start_apache.sh
source start_tomcat.sh
