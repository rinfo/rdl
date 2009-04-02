#!/bin/bash

# Walk args (opt build classpath and/or set environment dir):
ENV_BASE=src/environments
ENV_DIR=
for arg in $@; do
    if [ "$arg" == "-b" ]; then
        mvn dependency:build-classpath -Dmdep.outputFile=classpath.txt
        continue
    fi
    env_candidate="$ENV_BASE/$arg"
    if [ -d $env_candidate ]; then
        ENV_DIR=$env_candidate
    else
        echo "Invalid env dir: $env_candidate"
    fi
done

# Use deps:
MVN_DEPS=$(cat classpath.txt)
# Add resources and groovy src (for test, main):
LOCAL=src/test/resources:src/test/groovy:src/main/resources:src/main/groovy:target/classes

# Then define classpath, including main sources and resources:
export CLASSPATH=$LOCAL:$MVN_DEPS:$ENV_DIR

