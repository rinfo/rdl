#!/bin/bash
TEST_SRC_DIR=$1
TEST_SERVER=$2 # http://localhost:8280
groovy map_feeds_docs.groovy ../../documentation/ $TEST_SRC_DIR $TEST_SERVER
groovy base_as_feed.groovy -b ../../resources/base/ -s $TEST_SRC_DIR -o $TEST_SRC_DIR #-l "/admin"

