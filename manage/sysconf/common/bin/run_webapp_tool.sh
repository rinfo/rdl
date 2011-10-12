#!/bin/bash
webapp=$1
shift
cd $webapp/WEB-INF/classes/ && JAVA_OPTS="-Xms256M -Xmx768M" java -cp $(for jar in $(ls ../lib/*.jar); do echo -n "$jar:"; done) $@
