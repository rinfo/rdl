"""
Diagnostics and admin tasks
"""
from __future__ import with_statement
import contextlib
import time
from fabric.api import *
from util import venv
from target import _needs_targetenv

@task
def can_i_deploy():
    """Tests password less sudo for automatic deploys."""
    sudo("echo 'it seems that I can run sudo as '", shell=False)
    sudo("whoami", shell=False)

@task
def list_dist():
    _needs_targetenv()
    run("ls -latr %(dist_dir)s/"%env)

@task
def clean_dist():
    _needs_targetenv()
    run("rm -rf %(dist_dir)s/*"%env)

@task
def tail():
    _needs_targetenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f"%env)

@task
def tail2():
    _needs_targetenv()
    run("tail -f %(tomcat)s/logs/localhost.%(datestamp)s.log"%env)

@task
def restart_all():
    _needs_targetenv()
    sudo("/etc/init.d/apache2 stop")
    restart_tomcat()
    sudo("/etc/init.d/apache2 start")

@contextlib.contextmanager
def _managed_tomcat_restart(wait=5, headless=False, force_start=False):
    _needs_targetenv()
    with settings(warn_only=True):
        result = sudo("%(tomcat_stop)s" % env, shell=not headless)
    do_start = force_start or not result.failed
    yield
    if do_start:
        print "... restarting in",
        for i in range(wait, 0, -1):
            print "%d..." % i,
            time.sleep(1)
        print
        sudo("%(tomcat_start)s" % env, shell=not headless)

@task
def restart_tomcat():
    with _managed_tomcat_restart(force_start=True):
        pass

@task
def restart_apache():
    _needs_targetenv()
    #sudo("/etc/init.d/apache2 stop")
    #sudo("/etc/init.d/apache2 start")
    sudo("/etc/init.d/apache2 restart")

@task
def reload_apache():
    _needs_targetenv()
    sudo("/etc/init.d/apache2 graceful")

@task
def war_props(warname="ROOT"):
    _needs_targetenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war"
            " WEB-INF/classes/*.properties"%venv())

