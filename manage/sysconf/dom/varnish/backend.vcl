# This file is included by rinfo-service.vcl

# Set rinfo-service backend for TEST environment
backend default {
	.host = "service.t1.lagr.dev.dom.se";
}