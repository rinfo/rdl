#!/bin/bash

echo '------------- Install Rinfo-Admin multi node'

apt-get update
source ./bootstrap.sh
source ./install_apache.sh
source ./install_admin.sh
source start_apache.sh
