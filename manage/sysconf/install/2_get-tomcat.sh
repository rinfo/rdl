#!/bin/bash
TOMCAT_DIST_BASE=http://www.apache.org/dist/tomcat
TOMCAT_MIRROR_BASE=http://apache.mirrors.spacedump.net/tomcat

version=$1
major=${version%%\.*}

tomcat_keys=$TOMCAT_DIST_BASE/tomcat-$major/KEYS
tomcat_pkg=apache-tomcat-${version}.tar.gz
dist=$TOMCAT_MIRROR_BASE/tomcat-$major/v${version}/bin/$tomcat_pkg

test -e $tomcat_pkg || wget $dist
wget $tomcat_keys
wget $TOMCAT_DIST_BASE/tomcat-$major/v${version}/bin/$tomcat_pkg.asc
wget $TOMCAT_DIST_BASE/tomcat-$major/v${version}/bin/$tomcat_pkg.md5

pgp -ka KEYS
pgp $tomcat_pkg.asc
cat $tomcat_pkg.md5 | xargs | md5sum -c

