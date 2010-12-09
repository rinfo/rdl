from __future__ import with_statement
from fabric.api import *
from exceptions import OSError
from util import venv
from targetenvs import _needs_targetenv
from servertools import _managed_tomcat_restart

@runs_once
def local_lib_rinfo_pkg():
    local("cd %(java_packages)s/ && mvn install"%env, capture=False)

def _deploy_war(localwar, warname):
    _needs_targetenv()
    put(localwar, "%(dist_dir)s/%(warname)s.war"%venv())
    with _managed_tomcat_restart():
        sudo("rm -rf %(tomcat_webapps)s/%(warname)s/"%venv())
        sudo("mv %(dist_dir)s/%(warname)s.war %(tomcat_webapps)s/"%venv())
        sudo("chown %(tomcat_user)s %(tomcat_webapps)s/%(warname)s.war"%venv())

