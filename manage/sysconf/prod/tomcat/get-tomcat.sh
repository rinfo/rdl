#!/bin/bash
TOMCAT_DIST_BASE=http://www.apache.org/dist/tomcat
TOMCAT_MIRROR_BASE=http://apache.dataphone.se/tomcat
TOMCAT_KEYS=$TOMCAT_DIST_BASE/tomcat-6/KEYS

version=$1

tomcat_pkg=apache-tomcat-${version}.tar.gz
dist=$TOMCAT_MIRROR_BASE/tomcat-6/v${version}/bin/$tomcat_pkg

test -e $tomcat_pkg || wget $dist
wget $TOMCAT_KEYS
wget $TOMCAT_DIST_BASE/tomcat-6/v${version}/bin/$tomcat_pkg.asc
wget $TOMCAT_DIST_BASE/tomcat-6/v${version}/bin/$tomcat_pkg.md5

pgp -ka KEYS
pgp $tomcat_pkg.asc
cat $tomcat_pkg.md5 | xargs | md5sum -c

