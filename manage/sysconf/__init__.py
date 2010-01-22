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
from fabric.contrib.files import exists
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from util import mkdirpath, slashed


SCRIPT_DIR = p.dirname(__file__)


def pull_etckeeper_repos():
    pass

@roles('main')
@runs_once
def sync_workdir():
    mkdirpath(env.mgr_workdir)
    rsync_project(slashed(env.mgr_workdir),
            p.join(SCRIPT_DIR, env.target, "install"), exclude=".*", delete=True)
    rsync_project(slashed(env.mgr_workdir),
            p.join(SCRIPT_DIR, env.target, "etc"), exclude=".*", delete=False)

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
    with cd("%(mgr_workdir)s/install/"%env):
        sudo("bash install-tomcat.sh %(tomcat_version)s"%env)

    with cd("%(mgr_workdir)s/etc/"%env):
        #$ cp -i init.d/tomcat /etc/init.d/
        pass
    #$ chmod 0755 /etc/init.d/tomcat
    #$ update-rc.d tomcat defaults

    with cd("%(mgr_workdir)s/etc/"%env):
        #$ cp -i apache2/workers.properties /etc/apache2/
        #$ cp -i apache2/conf.d/jk.conf /etc/apache2/conf.d/
        pass
    #$ chown root:root /etc/apache2/conf.d/jk.conf
    #$ /etc/init.d/apache2 stop
    #$ /etc/init.d/tomcat restart
    #$ /etc/init.d/apache2 start
    ##$ /etc/init.d/apache2 restart

