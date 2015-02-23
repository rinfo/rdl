import sys
from fabric.api import *
from fabric.contrib.files import exists
from fabfile.util import venv, exit_on_error
from fabfile.app import local_lib_rinfo_pkg
from fabfile.app import _deploy_war_norestart
from fabfile.target import _needs_targetenv
from fabfile.server import restart_apache
from fabfile.server import restart_tomcat
from fabfile.server import tomcat_stop
from fabfile.server import tomcat_start
from fabfile.util import msg_sleep
from fabfile.util import verify_url_content
from fabfile.util import test_url
from fabfile.util import JUnitReport


##
# Local build

@task
@runs_once
def package(deps="1", test="1"):
    """Builds and packages the rinfo-main war, configured for the target env."""
    if int(deps):
        local_lib_rinfo_pkg(test)
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
    if not exists(env.target_config_dir):
        sudo("mkdir %(target_config_dir)s" % env)
    put("%(java_packages)s/rinfo-main/src/environments/%(target)s/rinfo-main.properties"  % env,"%(target_config_dir)srinfo-main.properties"  % env, use_sudo=True)

@task
@roles('main')
@exit_on_error
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
    report = JUnitReport()
    url = "http://"+env.roledefs['main'][0]
    test_url(report, "Verify feed exists and contains 'tag:lagrummet.se,2009:rinfo'", "main.feed",
             "%(url)s/feed/current" % venv(), "tag:lagrummet.se,2009:rinfo")
    test_url(report, "Verify dataset exists and contains 'tag:lagrummet.se,2009:rinfo'", "main.dataset",
             "%(url)s/sys/dataset/rdf" % venv(), "tag:lagrummet.se,2009:rinfo")
    if not report.empty():
        file_name = "%(projectroot)s/testreport/main_test_report.log" % venv()
        report.create_report(file_name)
        print "Created report '%s'" % file_name


@task
@roles('main')
def ping_start_collect_admin():
    _needs_targetenv()
    feed_url = ''
    if env.target=='regression':
        feed_url = "http://%s/feed/current.atom" % env.roledefs['demosource'][0]
    else:
        feed_url = "http://%s/feed/current" % env.roledefs['admin'][0]
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    if not verify_url_content(" --data 'feed=%(feed_url)s' %(collector_url)s" % vars(), "Scheduled collect of"):
        raise Exception("Test failed")


@task
@roles('main')
def ping_start_collect_feed():
    _needs_targetenv()
    feed_url = "http://%s/feed/current.atom" % env.roledefs['demosource'][0]
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    if not verify_url_content(" --data 'feed=%(feed_url)s' %(collector_url)s" % vars(), "Scheduled collect of"):
        raise Exception("Test failed")


@task
@roles('main')
def clean():
    """ Cleans main from system """
    tomcat_stop()
    #sudo("rm -rf %(tomcat_webapps)s/rinfo-main" % venv())
    #sudo("rm -rf %(tomcat_webapps)s/rinfo-main.war" % venv())
    sudo("rm -rf %(tomcat)s/logs/rinfo-main*.*" % venv())
    #sudo("rm -rf %(rinfo_main_store)s/" % venv())
    tomcat_start()


@task
@roles('main')
def install_regression_data(restart_tomcat=True):
    """ Cleans main from system """
    if restart_tomcat:
        tomcat_stop()
    sudo("rm -rf %(rinfo_main_store)s/" % venv())
    sudo("mkdir %(rinfo_main_store)s/" % venv())
    with lcd("%(rinfo_main_store)s/" % venv()):
        sudo("tar xzvf /home/%(user)s/regression_test_data/rinfo_store.tar.gz" % venv())
        sudo("chown tomcat:root -R %(rinfo_main_store)s/" % venv())
    if restart_tomcat:
        tomcat_start()


@task
@roles('main')
def test_all():
    all(deps="0", test="0")
    restart_apache()
    #if env.target=='regression':
    #    install_regression_data()
    #else:
    restart_tomcat()
    msg_sleep(60, "restart apache, tomcat and wait for service to start, and depot consistency check")
    try:
        #ping_start_collect_admin()
        #msg_sleep(10,"collect feed")
        #ping_start_collect_feed()
        #msg_sleep(60,"collect feed")
        test()
    except:
        e = sys.exc_info()[0]
        print e
        sys.exit(1)
    finally:
        clean()