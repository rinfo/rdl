"""
Diagnostics and admin tasks
"""
from fabric.api import *
from util import venv
from targetenvs import _needs_targetenv


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

def restart_tomcat():
    _needs_targetenv()
    sudo("%(tomcat_stop)s"%env)
    sudo("%(tomcat_start)s"%env)

def restart_apache():
    _needs_targetenv()
    #sudo("/etc/init.d/apache2 stop")
    #sudo("/etc/init.d/apache2 start")
    sudo("/etc/init.d/apache2 restart")

def war_props(warname="ROOT"):
    _needs_targetenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war"
            " WEB-INF/classes/*.properties"%venv())

