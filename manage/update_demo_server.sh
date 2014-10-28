#!/bin/bash

if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

START_TIME=$SECONDS
#fab -p $PW_RINFO target.demo app.admin.all app.main.all app.checker.all app.service.all
#fab -p $PW_RINFO target.demo app.admin.all app.main.all:deps=0,test=0 app.checker.all:deps=0,test=0 app.service.all:deps=0,test=0
fab -p $PW_RINFO target.demo app.admin.all app.main.all:deps=0 app.checker.all:deps=0 app.service.all:deps=0

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

fab -p $PW_RINFO target.demo -R main server.restart_tomcat

ELAPSED_TIME=$((SECONDS-START_TIME))

echo "Elapsed time: $ELAPSED_TIME"

