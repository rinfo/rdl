# Test site regression style
# This setup is thought to be run periodically possibly triggered from soruce code change/checkin/commit
# Must run "install_regression_server.sh" first to prepare server
# If you want no password prompts, you must allso install a public key on the target server

# Enter global password first
if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

EXIT_STATUS=0

function exit_on_error
{
if [ $EXIT_STATUS -ne 0 ];then	
   echo "$1 returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi
}


# Test Admin
fab -p $PW_RINFO target.regression -R admin app.admin.testAll
EXIT_STATUS=$?
exit_on_error "Admin module"

# Test Checker
fab -p $PW_RINFO target.regression -R checker app.checker.testAll
EXIT_STATUS=$?
exit_on_error "Checker module"


