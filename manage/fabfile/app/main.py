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
    """Builds and packages the rinfo-main war, configured for the target env."""
    if int(deps): local_lib_rinfo_pkg()
    _needs_targetenv()
    flags = "" if int(test) else "-Dmaven.test.skip=true"
    local("cd %(java_packages)s/rinfo-main/ && "
            "mvn %(flags)s -P%(target)s clean package war:war" % venv(), capture=False)

##
# Server deploy

@task
@runs_once
@roles('main')
def setup():
    """Creates neccessary directories for rinfo-main runtime data."""
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s" % env)
    if not exists(env.rinfo_dir):
        sudo("mkdir %(rinfo_dir)s" % env)
    if not exists(env.rinfo_main_store):
        sudo("mkdir %(rinfo_main_store)s" % env)
        sudo("chown -R %(tomcat_user)s %(rinfo_main_store)s" % env)

@task
@roles('main')
def deploy(headless="0"):
    """Deploys the rinfo-main war package to target env."""
    setup()
    _deploy_war("%(java_packages)s/rinfo-main/target/rinfo-main-%(target)s.war" % env,
            "rinfo-main", int(headless))

@task
@roles('main')
def all(deps="1", test="1", headless="0"):
    """Packages and deploys the rinfo-main war to target env."""
    package(deps, test)
    deploy(headless)

##
# Server Maintainance

#@task
#@roles('main')
#def clear_collect_log(force="0"):
#    raise NotImplementedError
#    # TODO: make **SURE** this is whay you really want to do!
#    #sudo("rm -rf %(rinfo_main_store)s/collector-log" % env, user=env.tomcat_user)

