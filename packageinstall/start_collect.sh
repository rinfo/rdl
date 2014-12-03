#!/bin/bash

curl --data 'feed=http://rinfo.'$1'/feed/current' http://service.$1/collector
