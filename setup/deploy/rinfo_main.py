from usefab import *
from envs import *
from envs import _deploy_war

##
# Local build

@runs_once
def package_main(deps="1"):
    if int(deps): install_rinfo_pkg()
    require('deployenv', provided_by=deployenvs)
    local(v("cd ${java_packages}/rinfo-main/; mvn -P${deployenv} clean package"), capture=False)

##
# Server deploy

@runs_once
def setup_main():
    main()
    if not exists(env.dist_dir): run(v("mkdir $dist_dir"))
    if not exists(env.rinfo_dir): sudo(v("mkdir $rinfo_dir"))
    if not exists(env.rinfo_main_store):
        sudo(v("mkdir $rinfo_main_store"))
        sudo(v("chown -R ${tomcat_user} ${rinfo_main_store}"))

@runs_once
def deploy_main_resources():
    setup_main()
    # TODO: bundle necessary files (via pom, and adapt the paths in properties)
    tarname = v("${project}-${timestamp}.tar.gz")
    tmp_tar = v("/tmp/$tarname")
    dest_tar = v("${dist_dir}/rinfo-base.tar.gz")
    local(v("tar -czf ${tmp_tar} ${base_data}"))
    try:
        put(tmp_tar, dest_tar)
        sudo(v("rm -rf ${rinfo_dir}/resources")) # TODO: +/base
        sudo(v("tar -C ${rinfo_dir} -xzf ${dest_tar}"))
    finally:
        local("rm %s" % tmp_tar)

@runs_once
def deploy_main():
    deploy_main_resources()
    _deploy_war(v("${java_packages}/rinfo-main/target/rinfo-main-${deployenv}.war"),
            "rinfo-main")

def main_all(deps="1"):
    package_main(deps)
    deploy_main()

##
# Diagnostics

def ping_main_collector():
    require('host_map', provided_by=deployenvs)
    collector_url = "http://%s/collector/" % env.host_map['main'][0]
    feed_url = "http://%s:8182/feed/current" % env.host_map['testsources'][0]
    local(v("curl --data 'feed=${feed_url}' ${collector_url}"))

def ping_service_collector():
    require('host_map', provided_by=deployenvs)
    collector_url = "http://%s/collector/" % env.host_map['service'][0]
    feed_url = "http://%s/feed/current" % env.host_map['main'][0]
    local(v("curl --data 'feed=${feed_url}' ${collector_url}"))

