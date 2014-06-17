#!/bin/sh
# Installs all necessary compontents for a server to act as a regression test server

echo "Enter sudo password: "
read pwd

fab -p $pwd target.regression -R main,service,checker,admin sysconf.install_server sysconf.configure_server
fab -p $pwd target.regression -R main,service,checker,admin app.service.install_elasticsearch
fab -p $pwd target.regression -R main,service,checker,admin app.service.start_elasticsearch
fab -p $pwd target.regression -R main,service,checker,admin app.service.deploy_sesame


