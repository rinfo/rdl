#!/bin/bash

TEST_SRC_DIR=/opt/work/rinfo/testsources/www
TEST_SERVER=http://localhost:8280

if [[ -n "$1" ]]; then
    TEST_SRC_DIR=$1
fi

if [[ -n "$2" ]]; then
    TEST_SERVER=$2
fi

groovy map_feeds_docs.groovy ../../documentation/ $TEST_SRC_DIR $TEST_SERVER
groovy base_as_feed.groovy -b ../../resources/base/ -s $TEST_SRC_DIR -o $TEST_SRC_DIR #-l "/admin"

