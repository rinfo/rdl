# This file is included by rinfo-service.vcl

# Set host for TEST environment to enable virtualhost routing
set req.http.host = "service.test.lagrummet.se";