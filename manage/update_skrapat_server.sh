#!/bin/sh
if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

fab -p $PW_RINFO target.skrapat app.admin.all app.main.all app.checker.all app.service.all

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

fab -p $PW_RINFO target.skrapat -R main server.restart_tomcat


