load("deploy/envs.py")

##
# Local build

def install_libs():
    cd_pkg = "cd $(java_packages)/%s/; %s"
    local(cd_pkg % ('rinfo-base', 'mvn install'))
    local(cd_pkg % ('rinfo-store', 'mvn install'))

@depends(install_libs)
def package_main():
    local("cd $(java_packages)/rinfo-main/; mvn -P$(env) package")

##
# Server deploy

@depends(main)
def setup_main():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir $(rinfo_dir)", fail='ignore')
    sudo("mkdir $(rinfo_main_store)", fail='ignore')

@depends(setup_main)
def deploy_main_resources():
    # TODO: rsync instead? Or bundle (via pom)?
    tarname = "$(project)-$(fab_timestamp).tar"
    tmp_tar ="/tmp/%s" % tarname
    dest_tar = "$(dist_dir)/%s" % tarname
    local("tar -czf %s %s" % (tmp_tar, config.base_data))
    try:
        put(tmp_tar, dest_tar, fail='warn')
        sudo("rm -rf $(rinfo_dir)/resources", fail='warn') # TODO: +/base
        sudo("tar -C $(rinfo_dir) -xzf %s" % dest_tar)
    finally:
        local("rm %s" % tmp_tar)

@depends(deploy_main_resources)
def deploy_main():
    put("$(java_packages)/rinfo-main/target/rinfo-main-1.0-SNAPSHOT.war",
            '$(dist_dir)/ROOT.war')
    sudo("$(tomcat_stop)", fail='warn')
    sudo("rm -rf $(tomat_webapps)/ROOT/")
    sudo("mv $(dist_dir)/ROOT.war $(tomat_webapps)/ROOT.war")
    sudo("$(tomcat_start)")

@depends(package_main, deploy_main)
def main_all(): pass

##
# Diagnostics

@requires('host_map', provided_by=[staging, production])
def ping_main_collector():
    collector_url = "http://%s:8080/collector/" % config.host_map['main'][0]
    feed_url = "http://%s:8080/feed/current" % config.host_map['testsources'][0]
    local("curl --data 'feed=%s' %s" % (feed_url, collector_url))

