#!/bin/bash

version=$1

tar xzvf apache-tomcat-${version}.tar.gz
mv apache-tomcat-${version} /opt/
pushd /opt/
    ln -s apache-tomcat-${version} tomcat
    chmod 0755 /etc/init.d/tomcat
    update-rc.d tomcat defaults
popd

