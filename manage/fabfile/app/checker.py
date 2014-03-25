import sys
import time
from fabric.api import *
from fabric.contrib.files import exists
from fabfile.util import venv
from fabfile.app import local_lib_rinfo_pkg, _deploy_war, _deploy_war_norestart
from fabfile.target import _needs_targetenv
from fabfile.server import restart_apache
from fabfile.server import tomcat_stop
from fabfile.server import tomcat_start
from fabfile.util import msg_sleep
from fabfile.util import verify_url_content

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
    _deploy_war_norestart("%(java_packages)s/rinfo-checker/target/rinfo-checker-%(target)s.war"%env,
            "rinfo-checker", int(headless))

@task
@roles('checker')
def all(deps="1", test="1", headless="0"):
    package(deps, test)
    deploy(headless)

@task
@roles('checker')
def test():
    """Test functions of checker"""
    admin_url = "http://%s/" % env.roledefs['checker'][0]
    print "curl %(admin_url)s" % vars()
    respHttp = local("curl %(admin_url)s" % vars(), capture=True)
    if not "<h1>RInfo Checker" in str(respHttp):
        print "Could not find <h1>RInfo Checker in response! Failed!"
        print "#########################################################################################"
        print respHttp
        print "#########################################################################################"
        raise
    # Should test the response to validate the admin servers correctness

@task
@roles('checker')
def clean():
    sudo("rm -rf %(tomcat_webapps)s/rinfo-checker" % venv())
    sudo("rm -rf %(tomcat_webapps)s/rinfo-checker.war" % venv())

@task
@roles('admin')
def test_all():
    all(test="0")
    restart_apache()
    msg_sleep(10,"restart apache")
    try:
        test()
    except:
        e = sys.exc_info()[0]
        print e
        sys.exit(1)
    finally:
        tomcat_stop
        clean()
        tomcat_start