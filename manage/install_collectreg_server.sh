#!/bin/sh
# Installs all necessary compontents for a server to act as a regression test server

fab target.collectreg sysconf.install_server sysconf.configure_server
fab target.collectreg app.service.install_elasticsearch
fab target.collectreg app.service.start_elasticsearch
fab target.collectreg app.service.deploy_sesame


