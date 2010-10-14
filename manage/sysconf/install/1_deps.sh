#!/bin/bash
sudo apt-get install apache2 -y
sudo /usr/sbin/a2enmod proxy_ajp
sudo /usr/sbin/a2enmod proxy_http
sudo apt-get install pgp -y
