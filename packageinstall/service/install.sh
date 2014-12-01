#!/bin/bash

echo '------------- Install Rinfo-service multi node'

apt-get update
source ./bootstrap.sh
source ./install_apache.sh
source ./install_tomcat.sh

source ./install_service.sh
source ./install_sesame.sh

source ./install_varnish.sh
source ./install_elasticsearch.sh
source ./start_varnish.sh
source ./start_elasticsearch.sh

source start_apache.sh
source start_tomcat.sh

