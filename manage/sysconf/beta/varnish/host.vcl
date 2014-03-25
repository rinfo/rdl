# This file is included by rinfo-service.vcl

# Set host for BETA environment to enable virtualhost routing
set req.http.host = "service.beta.lagrummet.se";