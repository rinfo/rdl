#!/bin/bash
apt-get install apache2
a2enmod proxy_ajp
#http://www.debian.org/doc/manuals/debian-java-faq/ch7.html
apt-get install sun-java6-jdk
