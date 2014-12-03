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

rm tmp/install.sh
sed 's/dnsplaceholderforsed/'$1'/g' install.sh > tmp/install.sh

rm rinfo-rdl-singlenode.tar.gz

( cd tmp && tar -zcf ../rinfo-rdl-singlenode.tar.gz * )
