##
# Securing:
# See: <http://www.debian.org/doc/manuals/securing-debian-howto/ch-sec-services.en.html>
#
# Tomcat on Debian:
# See: <http://www.itoperationz.com/2009/06/how-to-install-and-configure-apache-tomcat-6-on-debian-5/>
# See: <http://www.debian.org/doc/manuals/debian-java-faq/ch7.html#s-openjdk>
#
# Tomcat configuration
# See: <http://tomcat.apache.org/connectors-doc-archive/jk2/proxy.html>
#
# Apache configuration
# See: <http://httpd.apache.org/docs/2.0/vhosts/name-based.html>
# See: <http://httpd.apache.org/docs/2.0/mod/core.html#namevirtualhost>
# See: <http://httpd.apache.org/docs/2.0/mod/mod_proxy.html#access>
from __future__ import with_statement
from os import path as p
from fabric.api import *
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from util import mkdirpath, slashed


SCRIPT_DIR = p.dirname(__file__)


def pull_etckeeper_repos():
    pass

@runs_once
@roles('main')
def sync_workdir():
    rsync_project(slashed(env.mgr_workdir), p.join(SCRIPT_DIR, env.target)+'/',
            exclude=".*", delete=True)

@runs_once
@roles('main')
def run_configure():
    with cd("%(mgr_workdir)s/install/"%env):
        sudo("bash configure.sh")

@roles('main')
def fetch_tomcat_dist():
    _needs_targetenv()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg"
    mkdirpath(workdir_tomcat)
    with cd(workdir_tomcat):
        run("bash %(mgr_workdir)s/install/get-tomcat.sh %(tomcat_version)s"%env)

@roles('main')
def install_tomcat():
    require('target', provided_by=targetenvs)
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg"
    mkdirpath(workdir_tomcat)
    with cd(workdir_tomcat):
        run("bash %(mgr_workdir)s/install/install-tomcat.sh %(tomcat_version)s"%env)

