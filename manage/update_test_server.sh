#!/bin/sh

fab target.test app.admin.all app.main.all app.checker.all app.service.all

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

fab target.test -R main server.restart_tomcat


