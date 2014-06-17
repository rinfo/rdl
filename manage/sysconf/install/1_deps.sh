#!/bin/bash

sudo apt-get update

# web server tools:
sudo apt-get install apache2 -y
sudo /usr/sbin/a2enmod proxy_ajp
sudo /usr/sbin/a2enmod proxy_http
sudo apt-get install pgp -y

# admin tools:
sudo apt-get install wget -y
sudo apt-get install curl -y
sudo apt-get install unzip -y
sudo apt-get install lsof -y
sudo apt-get install rsync -y

# java
sudo apt-get install openjdk-7-jdk -y

