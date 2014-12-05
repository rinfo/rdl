
rm -rf tmp
mkdir tmp

( cd ../../packages/java/rinfo-service &&
	mvn -Ptemplate clean package war:war )

( cd ../../packages/java/rinfo-service/target/rinfo-service/WEB-INF/classes &&
	sed -i 's/dnsplaceholderforsed/'$1'/g' rinfo-service.properties &&
	cd ../.. &&
	jar cvf ../rinfo-service.war . )

cp ../../packages/java/rinfo-service/target/rinfo-service.war tmp/

( cd ../../packages/java/rinfo-sesame-http &&
	mvn clean package war:war )

cp ../../packages/java/rinfo-sesame-http/target/dependency/sesame-workbench.war tmp/
cp ../../packages/java/rinfo-sesame-http/target/dependency/openrdf-sesame.war tmp/

cp ../../manage/sysconf/common/etc/init.d/tomcat tmp/init.d_tomcat

cp ../../manage/sysconf/template/www/robots.txt tmp/

cp ../../manage/sysconf/common/etc/apache2/workers.properties tmp/

cp ../../manage/sysconf/common/etc/apache2/conf.d/jk.conf tmp/

sed 's/dnsplaceholderforsed/'$1'/g' ../../manage/sysconf/template/etc/apache2/sites-available/service > tmp/service
sed 's/dnsplaceholderforsed/lagrummet\.se/g' ../../manage/sysconf/template/etc/apache2/sites-available/service >> tmp/service

cp ~/.ssh/id_rsa.pub tmp/

cp ../../manage/sysconf/common/varnish/rinfo-service.vcl tmp/rinfo-service.vcl
cp ../../manage/sysconf/test/varnish/backend.vcl tmp/backend.vcl
cp ../../manage/sysconf/test/varnish/host.vcl tmp/host.vcl
cp ../../manage/sysconf/test/etc/default/varnish tmp/varnish

cp ../prepare/es.tar.gz tmp/
cp ../../manage/sysconf/common/elasticsearch/elasticsearch.yml tmp/

cp ../reuse/bootstrap.sh tmp/
cp ../reuse/install_apache.sh tmp/
cp ../reuse/install_tomcat.sh tmp/
cp ../reuse/start_apache.sh tmp/
cp ../reuse/start_tomcat.sh tmp/

cp ./install_service.sh tmp/
cp ./install_sesame.sh tmp/
cp ./install_varnish.sh tmp/
cp ./install_elasticsearch.sh tmp/
cp ./start_varnish.sh tmp/
cp ./start_elasticsearch.sh tmp/

cp install.sh tmp/

rm rinfo-service.tar.gz

git log -1 | grep 'commit' > tmp/git_status.txt

( cd tmp && tar -zcf ../rinfo-service.tar.gz * )

