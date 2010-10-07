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


##
# Continuous Maintenance

@runs_once
def configure_server(sync="1"):
    if int(sync):
        sync_workdir()

    etc_dir = "%(mgr_workdir)s/etc/" % env

    if env.get('custom_tomcat'):
        with cd("%(tomcat)s" % env):
            sudo("chown -R %(tomcat_user)s webapps temp logs work conf" % env)
        with cd(etc_dir):
            if sudo("cp -vu init.d/tomcat /etc/init.d/"):
                sudo("chmod 0755 /etc/init.d/tomcat")
                sudo("update-rc.d tomcat defaults")

    with cd(etc_dir):
        if env.get('apache_jk_tomcat'):
            if sudo("cp -vu apache2/workers.properties /etc/apache2/"):
                sudo("chown root:root /etc/apache2/workers.properties")
            if sudo("cp -vu apache2/conf.d/jk.conf /etc/apache2/conf.d/"):
                sudo("chown root:root /etc/apache2/conf.d/jk.conf")
            sudo("cp -vu apache2/mods-available/proxy.conf /etc/apache2/mods-available/")

        for role in env.roles:
            sites = env.get('apache_sites')
            if not sites or role not in sites: continue
            for site in sites[role]:
                sudo("cp -vu apache2/sites-available/%s /etc/apache2/sites-available/" % site)
                sudo("a2ensite %s" % site)

@runs_once
def sync_workdir():
    local_conf_dir = p.join(SCRIPT_DIR, env.target)
    rsync_project(slashed(env.mgr_workdir), slashed(local_conf_dir),
            exclude=".*", delete=True)


def pull_etckeeper_repos():
    pass

##
# Initial Setup

def fetch_tomcat_dist():
    _needs_targetenv()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    mkdirpath(workdir_tomcat)
    with cd(workdir_tomcat):
        run("bash %(mgr_workdir)s/install/get-tomcat.sh %(tomcat_version)s" % env)

def install_tomcat():
    _needs_targetenv()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    mkdirpath(workdir_tomcat)
    with cd(workdir_tomcat):
        sudo("bash %(mgr_workdir)s/install/install-tomcat.sh %(tomcat_version)s" % env)

