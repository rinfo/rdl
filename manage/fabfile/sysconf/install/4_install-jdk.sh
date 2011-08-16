#!/bin/bash
/bin/echo "sun-java6-plugin shared/accepted-sun-dlj-v1-1 boolean true" | /usr/bin/debconf-set-selections
sudo apt-get install sun-java6-jdk -y
