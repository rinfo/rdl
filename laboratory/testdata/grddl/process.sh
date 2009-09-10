#!/bin/bash

if [ $# == 0 ]; then
    echo "Example usages:"
    echo "  $ $0 socialstyrelsen/examples/sosfs-2009-12.html  socialstyrelsen/html_to_rdfa.xslt rdfa"
    echo "  $ $0 socialstyrelsen/examples/sosfs-2009-12.html socialstyrelsen/html_to_rdfxml.xslt xml"
    echo "With an URL:"
    echo "  $ $0 http://www.socialstyrelsen.se/sosfs/2009-12 socialstyrelsen/html_to_rdfxml.xslt xml"
    exit 0
fi

INPUT=$1
XSLT=$2

if [[ "$INPUT" =~ "http" ]]; then
    GET_SOURCE="curl -s"
else
    GET_SOURCE="cat"
fi
if [ "$3" == "rdfa" ]; then
    POST_PROCESS="rdfpipe -irdfa -on3 -"
elif [ "$3" == "xml" ]; then
    POST_PROCESS="rdfpipe -on3 -"
else
    POST_PROCESS="xmllint --format -"
fi

$GET_SOURCE $INPUT | tidy -asxhtml -n -q -utf8 - 2>/dev/null | xsltproc $XSLT - | $POST_PROCESS

