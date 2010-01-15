#!/bin/bash

checksums() {
    real=$(md5 -q $1/content.rdf)
    expected=$(perl -ne 'print $1 if s/le:md5="([^"]+)"/$1/g' $1/local-meta/collector-via.entry)
    if [[ $real != $expected ]]; then
        echo $1
        echo "    MD5 is $real, expected $expected"
    fi
}

depot=$1

for file in $(find "$depot" -name 'ENTRY-INFO'); do
    checksums $file
done

