#!/bin/sh
# Test site regression style
# This setup is thought to be run periodically possibly triggered from soruce code change/checkin/commit
# Must run "install_regression_server.sh" first to prepare server
# If you want no password prompts, you must allso install a public key on the target server

# Enter global password first
if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

# Test Admin
fab -p $PW_RINFO target.regression -R admin app.admin.test_all
EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then	
   echo "Admin module returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

# Test Checker
#fab -p $PW_RINFO target.regression -R checker app.checker.test_all
#EXIT_STATUS=$?
#if [ $EXIT_STATUS -ne 0 ];then
#   echo "Checker module returned $EXIT_STATUS! Exiting!"
#   exit $EXIT_STATUS
#fi

# Test Service
fab -p $PW_RINFO target.regression -R service app.service.test_all
EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then	
   echo "Service module returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

# Test Main
fab -p $PW_RINFO target.regression -R main app.main.test_all
EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then	
   echo "Main module returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi



