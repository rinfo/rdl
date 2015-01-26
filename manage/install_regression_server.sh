#!/bin/sh
# Installs all necessary compontents for a server to act as a regression test server

fab target.regression -R main,service,checker,admin sysconf.install_server sysconf.configure_server
fab target.regression -R main,service,checker,admin app.service.install_elasticsearch
fab target.regression -R main,service,checker,admin app.service.start_elasticsearch
fab target.regression -R main,service,checker,admin app.service.deploy_sesame


