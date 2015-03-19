
rm -rf tmp
mkdir -p tmp/output

cp ../../manage/sysconf/template/etc/apache2/sites-available/admin tmp/

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

cp install.sh tmp/

( cd ../../tools/rinfomain &&
	groovy base_as_feed.groovy -b ../../resources/base/ -s ../../resources/template/datasources.n3 -o ../../packageinstall/admin/tmp/output
)

cp ../reuse/install_apache.sh tmp/
cp ../reuse/start_apache.sh tmp/

cp ./install_admin.sh tmp/

git log -1 | grep 'commit' > tmp/git_status.txt

( cd tmp && tar -zcf ../rinfo-admin.tar.gz * )

