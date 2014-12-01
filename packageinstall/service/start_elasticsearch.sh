#!/bin/bash 

echo '------------- Start Elastic Search'

/opt/work/rinfo/elasticsearch/bin/elasticsearch -D es.config=/opt/elasticsearch/config/elasticsearch.yml &
