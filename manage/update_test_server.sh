#!/bin/sh
if [ -z "$PW_RINFO" ]; then
	echo "Enter sudo password: "
	read PW_RINFO
fi

fab -p $PW_RINFO target.test app.admin.all app.main.all app.checker.all app.service.all
fab -p $PW_RINFO target.test -R main server.restart_tomcat


