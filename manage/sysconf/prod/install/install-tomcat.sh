#!/bin/bash

version=$1

tar xzvf apache-tomcat-${version}.tar.gz
mv apache-tomcat-${version} /opt/
pushd /opt/
    ln -s apache-tomcat-${version} tomcat
popd

