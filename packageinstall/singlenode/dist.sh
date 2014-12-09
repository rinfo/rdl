#!/bin/bash

rm -rf tmp
mkdir tmp

( cd ../admin && source dist.sh $1 )
( cd ../rinfo && source dist.sh $1 )
( cd ../service && source dist.sh $1 )

cp -r ../service/tmp/* tmp/
rm tmp/git_status.txt
rm tmp/install.sh

cp ../admin/install_admin.sh tmp/
cp ../admin/tmp/admin tmp/
cp -r ../admin/tmp/output tmp/

cp ../rinfo/install_rinfo.sh tmp/
cp ../rinfo/tmp/rinfo-main tmp/
cp -r ../rinfo/tmp/rinfo-main.war tmp/
cp -r ../rinfo/tmp/workers.properties tmp/

cp ../checker/install_checker.sh tmp/
cp ../checker/tmp/checker tmp/
cp -r ../checker/tmp/rinfo-checker.war tmp/

sed 's/dnsplaceholderforsed/'$1'/g' install.sh > tmp/install.sh

rm rinfo-rdl-singlenode.tar.gz

git log -1 | grep 'commit' > tmp/git_status.txt

( cd tmp && tar -zcf ../rinfo-rdl-singlenode.tar.gz * )
