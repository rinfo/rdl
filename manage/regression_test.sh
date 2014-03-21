# Test site regression style
# This setup is thought to be run periodically possibly triggered from soruce code change/checkin/commit
# Must run "install_regression_server.sh" first to prepare server

# Enter global password first
if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

# Test Admin
fab -p $PW_RINFO target.regression -R admin app.admin.testAll
EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then	
   echo "Test returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

# Test Checker
fab -p $PW_RINFO target.regression -R checker app.checker.all
fab -p $PW_RINFO target.regression -R checker server.restart_apache
echo "Pause 2 min until checker install is complete"
sleep 120
fab -p $PW_RINFO target.regression -R checker app.checker.test
fab -p $PW_RINFO target.regression -R checker server.tomcat_stop
fab -p $PW_RINFO target.regression -R checker app.checker.clean
fab -p $PW_RINFO target.regression -R checker server.tomcat_start


