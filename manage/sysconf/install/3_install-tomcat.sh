#!/bin/bash

version=$1

tar xzvf apache-tomcat-${version}.tar.gz
mv apache-tomcat-${version} /opt/
pushd /opt/
    ln -s apache-tomcat-${version} tomcat
popd

# Remove the unnecessary default applications
rm -rf /opt/tomcat/webapps/*

useradd tomcat -g nogroup -d /opt/tomcat/ -s /bin/false -p'*' -r

