#!/bin/bash 

echo '------------- Install Elastic Search'

mkdir -p /opt/elasticsearch/config
cp elasticsearch.yml /opt/elasticsearch/config/elasticsearch.yml

chown -R root:root /opt/elasticsearch/config
chmod -R 644 /opt/elasticsearch/config

(
	cd /opt/work/rinfo/
	tar -zxf ~/es.tar.gz
	ln -s /opt/work/rinfo/elasticsearch-1.3.4/ elasticsearch
)
