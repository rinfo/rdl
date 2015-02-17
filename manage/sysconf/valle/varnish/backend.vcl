# This file is included by rinfo-service.vcl

# Set rinfo-service backend for TEST environment
backend default {
	.host = "service.valle.lagrummet.se";
}