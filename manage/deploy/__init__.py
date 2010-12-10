from __future__ import with_statement
from fabric.api import *
from exceptions import OSError
from util import venv
from targetenvs import _needs_targetenv
from servertools import _managed_tomcat_restart

@runs_once
def local_lib_rinfo_pkg():
    local("cd %(java_packages)s/ && mvn install"%env, capture=False)

def _deploy_war(localwar, warname, headless = False):
    _needs_targetenv()
    put(localwar, "%(dist_dir)s/%(warname)s.war"%venv())
    with _managed_tomcat_restart():
        run("rm -rf %(tomcat_webapps)s/%(warname)s/"%venv())
        run("unzip -q %(dist_dir)s/%(warname)s.war -d %(tomcat_webapps)s/%(warname)s"%venv())
        #run("chmod -R go-w %(tomcat_webapps)s/%(warname)s"%venv())

