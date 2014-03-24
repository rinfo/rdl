# This file is included by rinfo-service.vcl

# Set rinfo-service backend for BETA environment
backend default {
	.host = "service.beta.lagrummet.se";
}