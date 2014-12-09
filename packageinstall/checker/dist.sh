
rm -rf tmp
mkdir tmp

( cd ../../packages/java/rinfo-checker &&
	mvn -Ptemplate clean package war:war )

( cd ../../packages/java/rinfo-checker/target/rinfo-checker/WEB-INF/classes &&
	sed -i 's/dnsplaceholderforsed/'$1'/g' rinfo-main.properties &&
	cd ../.. &&
	jar cvf ../rinfo-checker.war . )

cp ../../packages/java/rinfo-checker/target/rinfo-checker.war tmp/

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/workers.properties tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

sed 's/dnsplaceholderforsed/'$1'/g' ../../manage/sysconf/template/etc/apache2/sites-available/checker > tmp/checker
sed 's/dnsplaceholderforsed/lagrummet\.se/g' ../../manage/sysconf/template/etc/apache2/sites-available/checker >> tmp/checker

cp ~/.ssh/id_rsa.pub tmp/

cp ../reuse/bootstrap.sh tmp/
cp ../reuse/install_apache.sh tmp/
cp ../reuse/install_tomcat.sh tmp/
cp ./install_checker.sh tmp/
cp ../reuse/create_folders.sh tmp/
cp ../reuse/start_apache.sh tmp/
cp ../reuse/start_tomcat.sh tmp/

cp install.sh tmp/

git log -1 | grep 'commit' > tmp/git_status.txt

( cd tmp && tar -zcf ../rinfo-checker.tar.gz * )

