from fabric.api import *
from util import venv
from targetenvs import _needs_targetenv

##
# Diagnostics and admin tasks

def list_dist():
    _needs_targetenv()
    run("ls -latr %(dist_dir)s/"%env)

def clean_dist():
    _needs_targetenv()
    run("rm -rf %(dist_dir)s/*"%env)

def tail():
    _needs_targetenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f"%env)

def restart_tomcat():
    _needs_targetenv()
    sudo("%(tomcat_stop)s"%env)
    sudo("%(tomcat_start)s"%env)

def restart_apache():
    _needs_targetenv()
    # .. apache2ctl
    sudo("/etc/init.d/apache2 stop")
    sudo("/etc/init.d/apache2 start")

def war_props(warname="ROOT"):
    _needs_targetenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war"
            " WEB-INF/classes/*.properties"%venv())

