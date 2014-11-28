
rm -rf tmp
mkdir tmp

( cd ../../packages/java/rinfo-main &&
	mvn -Ptemplate clean package war:war )

( cd ../../packages/java/rinfo-main/target/rinfo-main/WEB-INF/classes &&
	sed -i 's/dnsplaceholderforsed/'$1'/g' rinfo-main.properties &&
	cd ../.. &&
	jar cvf ../rinfo-main.war . )

cp ../../packages/java/rinfo-main/target/rinfo-main.war tmp/

cp ../../manage/sysconf/common/etc/init.d/tomcat tmp/init.d_tomcat

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/workers.properties tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

sed 's/dnsplaceholderforsed/'$1'/g' ../../manage/sysconf/template/etc/apache2/sites-available/rinfo-main > tmp/rinfo-main

cp ~/.ssh/id_rsa.pub tmp/

cp install.sh tmp/

( cd tmp && tar -zcf ../rinfo-main.tar.gz * )

