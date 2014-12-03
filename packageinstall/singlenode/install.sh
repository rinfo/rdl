#!/bin/bash

echo '------------- Install Rinfo-RDL single node'

apt-get update
source ./bootstrap.sh
source ./install_apache.sh
source ./install_tomcat.sh

source ./install_admin.sh

source ./create_folders.sh
source ./install_rinfo.sh

source ./install_service.sh
source ./install_sesame.sh

source ./install_varnish.sh
source ./install_elasticsearch.sh
source ./start_varnish.sh
source ./start_elasticsearch.sh

export sym=dnsplaceholderforsed

echo '127.0.0.1	'$sym, admin.$sym, checker.$sym, service.$sym, rinfo.$sym >> /etc/hosts

source start_apache.sh
source start_tomcat.sh

