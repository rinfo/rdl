#!/bin/bash

echo '------------- Install Rinfo-main multi node CentOS'

source ./install_apache.sh
source ./install_tomcat.sh

source ./create_folders.sh
source ./install_rinfo.sh $1

source start_apache.sh
source start_tomcat.sh
