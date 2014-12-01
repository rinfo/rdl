#!/bin/bash 

echo '------------- Install Varnish'

echo 'deb http://repo.varnish-cache.org/debian/ wheezy varnish-3.0' >> /etc/apt/sources.list
apt-get install varnish -y

mkdir -p /opt/work/rinfo/varnish/
mkdir -p /opt/work/rinfo/varnish/cache

mkdir -p /opt/varnish/cache
chown -R rinfo:rinfo /opt/varnish
chmod -R 644 /opt/varnish
chown root:root /opt/varnish/cache
chmod 644 /opt/varnish/cache

cp rinfo-service.vcl /opt/varnish/rinfo-service.vcl
cp backend.vcl /opt/varnish/backend.vcl
cp host.vcl /opt/varnish/host.vcl

cp varnish /etc/default/

/etc/init.d/varnish stop

