#!/bin/bash

rm -rf tmp
mkdir tmp

( cd ../admin && source dist.sh $1 )
( cd ../rinfo && source dist.sh $1 )
( cd ../service && source dist.sh $1 )

cp -r ../service/tmp/* tmp/

cp ../admin/install_admin.sh tmp/
cp ../admin/tmp/admin tmp/
cp -r ../admin/tmp/output tmp/

cp ../rinfo/install_rinfo.sh tmp/
cp ../rinfo/tmp/rinfo-main tmp/
cp -r ../rinfo/tmp/rinfo-main.war tmp/
cp -r ../rinfo/tmp/workers.properties tmp/
cp -r ../rinfo/tmp/start_collect.sh tmp/

rm tmp/install.sh
cp install.sh tmp/

rm rinfo-rdl-singlenode.tar.gz

( cd tmp && tar -zcf ../rinfo-rdl-singlenode.tar.gz * )
