
rm -rf tmp
mkdir tmp

( cd ../../packages/java/rinfo-main && mvn -Ptemplate clean package war:war )

cp ../../packages/java/rinfo-main/src/environments/template/rinfo-main.properties tmp/

cp ../../packages/java/rinfo-main/target/rinfo-main-template.war tmp/rinfo-main.war

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/workers.properties tmp/

cp ../../manage/sysconf/template/etc/apache2/sites-available/rinfo-main tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

cp ../reuse/install_apache.sh tmp/
cp ../reuse/install_tomcat.sh tmp/
cp ./install_rinfo.sh tmp/
cp ../reuse/create_folders.sh tmp/
cp ../reuse/start_apache.sh tmp/
cp ../reuse/start_tomcat.sh tmp/

cp install.sh tmp/

git log -1 | grep 'commit' > tmp/git_status.txt

( cd tmp && chmod a+x *.sh && tar -zcf ../rinfo-main.tar.gz * )

