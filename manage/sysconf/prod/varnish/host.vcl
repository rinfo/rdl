# This file is included by rinfo-service.vcl

# Set host for PROD environment to enable virtualhost routing
set req.http.host = "service.lagrummet.se";