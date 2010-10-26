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
from os import path as p, sep
from fabric.api import *
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from util import mkdirpath, slashed


SCRIPT_DIR = p.dirname(__file__)


##
# Continuous Maintenance

def configure_server(sync="1"):
    if int(sync):
        _sync_workdir()
    configure_app_container(0)
    configure_sites(0)


def configure_app_container(sync="1"):
    if int(sync):
        _sync_workdir()

    common_etc_dir = "%(mgr_workdir)s/common/etc" % env

    if env.get('custom_tomcat'):
        with cd("%(tomcat)s" % env):
            sudo("chown -R %(tomcat_user)s webapps temp logs work conf" % env)
        with cd(common_etc_dir):
            if sudo("cp -vu init.d/tomcat /etc/init.d/"):
                sudo("chmod 0755 /etc/init.d/tomcat")
                sudo("update-rc.d tomcat defaults")

    with cd(common_etc_dir):
        if env.get('apache_jk_tomcat'):
            if sudo("cp -vu apache2/workers.properties /etc/apache2/"):
                sudo("chown root:root /etc/apache2/workers.properties")
            if sudo("cp -vu apache2/conf.d/jk.conf /etc/apache2/conf.d/"):
                sudo("chown root:root /etc/apache2/conf.d/jk.conf")

def configure_sites(sync="1"):
    if int(sync):
        _sync_workdir()

    env_etc_dir = "%(mgr_workdir)s/%(target)s/etc" % env

    with cd(env_etc_dir):
        for role in env.roles:
            sites = env.get('apache_sites')
            if not sites or role not in sites: continue
            for site in sites[role]:
                sudo("cp -vu apache2/sites-available/%s /etc/apache2/sites-available/" % site)
                sudo("a2ensite %s" % site)

@runs_once
def _sync_workdir():
    common_conf_dir = p.join(SCRIPT_DIR, "common")
    rsync_project(env.mgr_workdir, common_conf_dir,
            exclude=".*", delete=True)
    local_conf_dir = p.join(SCRIPT_DIR, env.target)
    rsync_project(env.mgr_workdir, local_conf_dir,
            exclude=".*", delete=True)


##
# Initial Setup

@runs_once
def _prepare_initial_setup():
    mkdirpath("%(mgr_workdir)s/install" % env)
    put(sep.join((env.projectroot, 'manage', 'sysconf', 'install', '*.sh')), "%(mgr_workdir)s/install" % env)

    mkdirpath("%(mgr_workdir)s/tomcat_pkg" % env)

def install_dependencies():
    _needs_targetenv()
    _prepare_initial_setup()
    sudo("bash %(mgr_workdir)s/install/1_deps.sh" % env)

def fetch_tomcat_dist():
    _needs_targetenv()
    _prepare_initial_setup()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    with cd(workdir_tomcat):
        run("bash %(mgr_workdir)s/install/2_get-tomcat.sh %(tomcat_version)s" % env)

def install_tomcat():
    _needs_targetenv()
    _prepare_initial_setup()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    with cd(workdir_tomcat):
        sudo("bash %(mgr_workdir)s/install/3_install-tomcat.sh %(tomcat_version)s" % env)

