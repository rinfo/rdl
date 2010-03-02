#!/bin/bash
sudo apt-get install apache2
sudo /usr/sbin/a2enmod proxy_ajp
#http://www.debian.org/doc/manuals/debian-java-faq/ch7.html ::
#   sudo vim /etc/apt/sources.list ::
#       deb <$HTTP-FTP...> $DIST main contrib non-free
#   sudo apt-get update
sudo apt-get install sun-java6-jdk
sudo apt-get install pgp
