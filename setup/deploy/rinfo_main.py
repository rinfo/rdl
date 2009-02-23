
##
# Local build

@requires('env', provided_by=sysenvs)
@depends(install_rinfo_pkg)
def package_main():
    local("cd $(java_packages)/rinfo-main/; mvn -P$(env) clean package")

##
# Server deploy

@depends(main)
def setup_main():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir $(rinfo_dir)", fail='ignore')
    sudo("mkdir $(rinfo_main_store)", fail='ignore')
    sudo("chown -R $(tomcat_user) $(rinfo_main_store)", fail='ignore')

@depends(setup_main)
def deploy_main_resources():
    # TODO: bundle necessary files (via pom, and adapt the paths in properties)
    tarname = "$(project)-$(fab_timestamp).tar.gz"
    tmp_tar ="/tmp/%s" % tarname
    dest_tar = "$(dist_dir)/rinfo-base.tar.gz"
    local("tar -czf %s $(base_data)" % tmp_tar)
    try:
        put(tmp_tar, dest_tar, fail='warn')
        sudo("rm -rf $(rinfo_dir)/resources", fail='warn') # TODO: +/base
        sudo("tar -C $(rinfo_dir) -xzf %s" % dest_tar)
    finally:
        local("rm %s" % tmp_tar)

@depends(deploy_main_resources)
def deploy_main():
    deploy_war(
            "$(java_packages)/rinfo-main/target/rinfo-main-$(env).war",
            "rinfo-main")

@depends(package_main, deploy_main)
def main_all(): pass

##
# Diagnostics

@requires('host_map', provided_by=sysenvs)
def ping_main_collector():
    collector_url = "http://%s/collector/" % config.host_map['main'][0]
    feed_url = "http://%s:8182/feed/current" % config.host_map['testsources'][0]
    local("curl --data 'feed=%s' %s" % (feed_url, collector_url))

@requires('host_map', provided_by=sysenvs)
def ping_service_collector():
    collector_url = "http://%s/collector/" % config.host_map['service'][0]
    feed_url = "http://%s/feed/current" % config.host_map['main'][0]
    local("curl --data 'feed=%s' %s" % (feed_url, collector_url))

