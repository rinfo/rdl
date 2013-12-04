fab -p $PW_RINFO target.demo app.admin.all app.main.all app.checker.all app.service.all
fab -p $PW_RINFO target.demo -R main server.restart_tomcat


