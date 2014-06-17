# This file is included by rinfo-service.vcl

# Set rinfo-service backend for PROD environment
backend default {
	.host = "service.lagrummet.se";
}