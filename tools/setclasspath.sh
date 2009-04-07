#!/bin/bash

generate_classpath_file() {
    outfile=$1/classpath.txt
    pkgdir=$2
    pushd $pkgdir
        mvn dependency:build-classpath -Dmdep.outputFile=$outfile
    popd
    echo -n ":$pkgdir/target/classes/" >> $outfile
    echo -n ":$pkgdir/src/main/groovy/" >> $outfile
}

if [ "$1" != "" ]; then
    generate_classpath_file $(pwd) $1
fi
export CLASSPATH=$(cat classpath.txt)

