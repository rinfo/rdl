#!/bin/bash

OUTDIR=$(pwd)
if [ "$1" != "" ]; then
    pushd $1
    mvn dependency:build-classpath -Dmdep.outputFile=$OUTDIR/classpath.txt
    popd
fi

MVN_DEPS=$(cat classpath.txt)
export CLASSPATH=$MVN_DEPS

