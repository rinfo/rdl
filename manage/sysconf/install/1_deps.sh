#!/bin/bash

sudo apt-get update

# web server tools:
sudo apt-get install apache2 -y
sudo /usr/sbin/a2enmod proxy_ajp
sudo /usr/sbin/a2enmod proxy_http
sudo apt-get install pgp -y

# admin tools:
sudo apt-get install wget
sudo apt-get install curl
sudo apt-get install unzip
sudo apt-get install lsof

# java
sudo apt-get install openjdk-6-jdk -y

