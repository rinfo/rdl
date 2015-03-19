#!/bin/bash

echo '------------- Install Rinfo-Admin multi node CentOS'

source ./install_apache.sh
source ./install_admin.sh $1
source start_apache.sh
