#!/bin/bash

# Walk args (opt build classpath and/or set environment dir):
ENV_BASE=src/environments
ENV_DIR=$ENV_BASE/default
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
# We need groovy runtime in front for groovysh to work:
GROOVY_JARS=$(for jar in $(ls $GROOVY_HOME/lib/*.jar); do echo -n "$jar:"; done)
# Add resources and groovy src:
#LOCAL=src/main/resources:src/main/groovy:src/test/resources:src/test/groovy
LOCAL=src/main/groovy:src/main/resources:target/classes:src/test/resources:src/test/groovy

# Then define classpath, including main sources and resources:
export CLASSPATH=$GROOVY_JARS:$LOCAL:$MVN_DEPS:$ENV_DIR
#export CLASSPATH=$MVN_DEPS:$ENV_DIR:$LOCAL

