#!/bin/sh

fab target.valle app.main.setup app.service.setup app.checker.setup app.service.ban_varnish
fab target.valle app.admin.all app.main.all app.checker.all app.service.all

EXIT_STATUS=$?
if [ $EXIT_STATUS -ne 0 ];then
   echo "fabric returned $EXIT_STATUS! Exiting!"
   exit $EXIT_STATUS
fi

fab target.valle -R main server.restart_tomcat


