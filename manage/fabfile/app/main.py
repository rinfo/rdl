import sys
from fabric.api import *
from fabric.contrib.files import exists
from fabfile.util import venv
from fabfile.app import local_lib_rinfo_pkg
from fabfile.app import _deploy_war
from fabfile.app import _deploy_war_norestart
from fabfile.target import _needs_targetenv
from fabfile.server import restart_apache
from fabfile.server import restart_tomcat
from fabfile.server import tomcat_stop
from fabfile.server import tomcat_start
from fabfile.util import msg_sleep
from fabfile.util import verify_url_content
from admin import ping_main


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
    _deploy_war_norestart("%(java_packages)s/rinfo-main/target/rinfo-main-%(target)s.war" % env,
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

@task
@roles('main')
def test():
    _needs_targetenv()
    admin_url = "http://%s/feed/current" % env.roledefs['main'][0]
    if not verify_url_content(admin_url,"<uri>http://rinfo.lagrummet.se/</uri>"):
        raise

@task
@roles('main')
def ping_start_collect_admin():
    _needs_targetenv()
    feed_url = "http://testfeed.lagrummet.se/admin/feed/current.atom"
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    if not verify_url_content("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars(),"Scheduled collect of"):
        raise

@task
@roles('main')
def ping_start_collect_feed():
    _needs_targetenv()
    feed_url = "http://testfeed.lagrummet.se/feed.atom"
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    if not verify_url_content("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars(),"Scheduled collect of"):
        raise



@task
@roles('main')
def clean():
    """ Cleans checker from system. Will assume tomcat is inactive """
    sudo("rm -rf %(tomcat_webapps)s/rinfo-main" % venv())
    sudo("rm -rf %(tomcat_webapps)s/rinfo-main.war" % venv())
    sudo("rm -rf %(rinfo_main_store)s/" % venv())

@task
@roles('main')
def test_all():
    all(test="0")
    restart_apache()
    #restart_tomcat()
    msg_sleep(15,"restart apache and wait for service to start")
    try:
        ping_start_collect_admin()
        msg_sleep(10,"collect feed")
        ping_start_collect_feed
        msg_sleep(60,"collect feed")
        test()
    except:
        e = sys.exc_info()[0]
        print e
        sys.exit(1)
    finally:
        tomcat_stop
        clean()
        tomcat_start


