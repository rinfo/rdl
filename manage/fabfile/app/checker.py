from fabric.api import *
from fabric.contrib.files import exists
from fabfile.util import venv
from fabfile.app import local_lib_rinfo_pkg, _deploy_war
from fabfile.target import _needs_targetenv

##
# Local build

@task
@runs_once
def package(deps="1", test="1"):
    if int(deps): local_lib_rinfo_pkg()
    _needs_targetenv()
    flags = "" if int(test) else "-Dmaven.test.skip=true"
    local("cd %(java_packages)s/rinfo-checker/ && "
            "mvn %(flags)s -P%(target)s clean package war:war" % venv(), capture=False)

##
# Server deploy

@task
@runs_once
@roles('checker')
def setup():
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)

@task
@roles('checker')
def deploy(headless="0"):
    setup()
    _deploy_war("%(java_packages)s/rinfo-checker/target/rinfo-checker-%(target)s.war"%env,
            "rinfo-checker", int(headless))

@task
@roles('checker')
def all(deps="1", test="1", headless="0"):
    package(deps, test)
    deploy(headless)

