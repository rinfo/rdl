from __future__ import with_statement
from fabric.api import *
from fabric.contrib.project import rsync_project
from fabfile.util import venv
from fabfile.target import _needs_targetenv
from fabfile.server import _managed_tomcat_restart


@runs_once
def local_lib_rinfo_pkg(test="1"):
    flags = "" if int(test) else "-Dmaven.test.skip=true"
    local("cd %(java_packages)s/ && mvn %(flags)s clean install " % venv(), capture=False)


def _deploy_war(localwar, warname, headless=False):
    _needs_targetenv()
    rsync_project("%(dist_dir)s/%(warname)s.war" % venv(), localwar, '--progress')
    with _managed_tomcat_restart(5, headless):
        run("rm -rf %(tomcat_webapps)s/%(warname)s/" % venv())
        run("unzip -q %(dist_dir)s/%(warname)s.war -d %(tomcat_webapps)s/%(warname)s" % venv())
        #run("chmod -R go-w %(tomcat_webapps)s/%(warname)s" % venv())
        #run("cp %(dist_dir)s/%(warname)s.war %(tomcat_webapps)s/." % venv())


def _deploy_war_norestart(localwar, warname, headless=False):
    _needs_targetenv()
    rsync_project("%(dist_dir)s/%(warname)s.war" % venv(), localwar, '--progress')
    run("chmod 644 %(dist_dir)s/%(warname)s.war" % venv())
    run("cp %(dist_dir)s/%(warname)s.war %(tomcat_webapps)s/." % venv())
    run("touch %(tomcat_webapps)s/%(warname)s.war" % venv())
