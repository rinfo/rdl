##
# Local build

@requires('env', provided_by=sysenvs)
@depends(install_rinfo_pkg)
def package_testapp():
    local("cd $(java_packages)/teststore-examples/; mvn -P$(env) package")

##
# Server deploy

@depends(testsources)
def setup_testsources():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir -p $(test_store)", fail='ignore')

@depends(setup_testsources)
def deploy_testdata():
    tarname = "example.org.tar.gz"
    dest_tar = "$(dist_dir)/%s" % tarname
    put("$(projectroot)/laboratory/testdata/%s" % tarname, dest_tar)
    sudo("rm -rf $(test_store)/example.org", fail='warn')
    sudo("tar -C $(test_store) -xzf %s" % dest_tar)

@depends(testsources)
def index_testdata():
    wdir = "$(tomcat_webapps)/ROOT/WEB-INF"
    clspath = '-cp $(for jar in $(ls lib/*.jar); do echo -n "$jar:"; done)'
    cmdclass = "se.lagrummet.rinfo.store.depot.FileDepotCmdTool"
    proppath = "classes/rinfo-depot.properties"
    #sudo("sh -c 'cd %s; java %s %s %s index'" % (wdir, clspath, cmdclass, proppath))
    #config.extend(vars())
    #sudo("sh -c 'cd $(wdir); java $(clspath) $(cmdclass) $(proppath) index'")
    sudo("sh -c 'cd %(wdir)s; java %(clspath)s %(cmdclass)s %(proppath)s index'"
            % vars())

@depends(testsources)
def list_testdata():
    run("ls $(test_store)/example.org/*/")

@depends(setup_testsources)
def deploy_testapp():
    put("$(java_packages)/teststore-examples/target/example-store-1.0-SNAPSHOT.war",
            "$(dist_dir)/ROOT.war")
    sudo("$(tomcat_stop)", fail='warn')
    sudo("rm -rf $(tomcat_webapps)/ROOT/")
    sudo("mv $(dist_dir)/ROOT.war $(tomcat_webapps)/ROOT.war")
    sudo("$(tomcat_start)")

@depends(package_testapp, deploy_testdata, index_testdata, deploy_testapp)
def testapp_all(): pass

