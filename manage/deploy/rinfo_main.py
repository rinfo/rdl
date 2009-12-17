from fabric.api import *
from fabric.contrib.files import exists
from deploy import local_lib_rinfo_pkg, _deploy_war
from targetenvs import *

##
# Local build

@runs_once
def package_main(deps="1"):
    if int(deps): local_lib_rinfo_pkg()
    require('target', provided_by=targetenvs)
    local("cd %(java_packages)s/rinfo-main/ && "
            "mvn -P%(target)s clean war:war"%env, capture=False)

##
# Server deploy

@runs_once
@roles('main')
def setup_main():
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    if not exists(env.rinfo_dir):
        sudo("mkdir %(rinfo_dir)s"%env)
    if not exists(env.rinfo_main_store):
        sudo("mkdir %(rinfo_main_store)s"%env)
        sudo("chown -R %(tomcat_user)s %(rinfo_main_store)s"%env)

@runs_once
@roles('main')
def deploy_main():
    setup_main()
    _deploy_war("%(java_packages)s/rinfo-main/target/rinfo-main-%(target)s.war"%env,
            "rinfo-main")

@roles('main')
def main_all(deps="1"):
    package_main(deps)
    deploy_main()

