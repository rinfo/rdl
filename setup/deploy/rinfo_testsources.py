from usefab import *

##
# Local build

def package_testapp(deps="1"):
    if int(deps): install_rinfo_pkg()
    require('deployenv', provided_by=deployenvs)
    local("cd ${java_packages}/teststore-examples/; mvn -P${env} package")

##
# Server deploy

def setup_testsources():
    testsources()
    run("mkdir ${dist_dir}", fail='ignore')
    sudo("mkdir -p ${test_store}", fail='ignore')

def deploy_testdata():
    setup_testsources()
    tarname = "example.org.tar.gz"
    dest_tar = "${dist_dir}/%s" % tarname
    put("${projectroot}/laboratory/testdata/%s" % tarname, dest_tar)
    sudo("rm -rf ${test_store}/example.org", fail='warn')
    sudo("tar -C ${test_store} -xzf %s" % dest_tar)

def index_testdata():
    testsources()
    wdir = "${tomcat_webapps}/ROOT/WEB-INF"
    clspath = '-cp $(for jar in $(ls lib/*.jar); do echo -n "$jar:"; done)'
    cmdclass = "se.lagrummet.rinfo.store.depot.FileDepotCmdTool"
    proppath = "classes/rinfo-depot.properties"
    sudo("sh -c 'cd %(wdir)s; java %(clspath)s %(cmdclass)s %(proppath)s index'"
            % vars())

def list_testdata():
    testsources()
    run("ls ${test_store}/example.org/*/")

def deploy_testapp():
    setup_testsources()
    put("${java_packages}/teststore-examples/target/example-store-1.0-SNAPSHOT.war",
            "${dist_dir}/ROOT.war")
    sudo("${tomcat_stop}", fail='warn')
    sudo("rm -rf ${tomcat_webapps}/ROOT/")
    sudo("mv ${dist_dir}/ROOT.war ${tomcat_webapps}/ROOT.war")
    sudo("${tomcat_start}")

def testapp_all(deps="1"):
    package_testapp(deps)
    deploy_testdata()
    index_testdata()
    deploy_testapp()

