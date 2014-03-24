# This file is included by rinfo-service.vcl

# Set rinfo-service backend for DEMO environment
backend default {
	.host = "service.demo.lagrummet.se";
}