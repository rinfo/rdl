"""
Diagnostics and admin tasks
"""
from __future__ import with_statement
import contextlib
import time
from fabric.api import *
from util import venv
from targetenvs import _needs_targetenv

def can_i_deploy():
    """Tests password less sudo for automatic deploys."""
    sudo("echo 'it seems that I can run sudo as '", shell=False)
    sudo("whoami", shell=False)

def list_dist():
    _needs_targetenv()
    run("ls -latr %(dist_dir)s/"%env)

def clean_dist():
    _needs_targetenv()
    run("rm -rf %(dist_dir)s/*"%env)

def tail():
    _needs_targetenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f"%env)

def tail2():
    _needs_targetenv()
    run("tail -f %(tomcat)s/logs/localhost.%(datestamp)s.log"%env)

def restart_all():
    _needs_targetenv()
    sudo("/etc/init.d/apache2 stop")
    restart_tomcat()
    sudo("/etc/init.d/apache2 start")

@contextlib.contextmanager
def _managed_tomcat_restart(wait=5, headless=False):
    _needs_targetenv()
    result = sudo("%(tomcat_stop)s" % env, term=not headless)
    if result.failed:
        raise OSError(result)
    yield
    print "... restarting in",
    for i in range(wait, 0, -1):
        print "%d..." % i,
        time.sleep(1)
    print
    sudo("%(tomcat_start)s" % env, term=not headless)

def restart_tomcat():
    with _managed_tomcat_restart(): pass

def restart_apache():
    _needs_targetenv()
    #sudo("/etc/init.d/apache2 stop")
    #sudo("/etc/init.d/apache2 start")
    sudo("/etc/init.d/apache2 restart")

def war_props(warname="ROOT"):
    _needs_targetenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war"
            " WEB-INF/classes/*.properties"%venv())

