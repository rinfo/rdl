from fabric.api import *
from fabric.contrib.files import exists
from deploy import local_lib_rinfo_pkg, _deploy_war
from targetenvs import _needs_targetenv

##
# Local build

@runs_once
def package_checker(deps="1"):
    if int(deps): local_lib_rinfo_pkg()
    _needs_targetenv()
    local("cd %(java_packages)s/rinfo-checker/ && "
            "mvn -P%(target)s clean package war:war"%env, capture=False)

##
# Server deploy

@runs_once
@roles('main')
def setup_checker():
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)

@roles('checker')
def deploy_checker(headless="0"):
    setup_checker()
    _deploy_war("%(java_packages)s/rinfo-checker/target/rinfo-checker-%(target)s.war"%env,
            "rinfo-checker", int(headless))

@roles('checker')
def checker_all(deps="1", headless="0"):
    package_checker(deps)
    deploy_checker(headless)

