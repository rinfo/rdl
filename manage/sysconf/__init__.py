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
from fabric.api import *
from fabric.contrib.files import exists
from targetenvs import _needs_targetenv
from util import dirpath


def pull_etckeeper_repos():
    pass

@roles('main')
def tomcat_to_target():
    _needs_targetenv()
    dirpath(env.mgr_work_tomcat)
    put("%(target)s/tomcat/get-tomcat.sh"%env, env.mgr_work_tomcat)
    with cd(env.mgr_work_tomcat):
        run("bash get-tomcat.sh %(tomcat_version)s"%env)

@roles('main')
def install_tomcat_at_target():
    require('target', provided_by=targetenvs)
    dirpath(env.mgr_work_tomcat)
    #rsync_project(TODO, slashed(env.mgr_work_tomcat), exclude=".*", delete=True)
    #put("tomcat/init-d-tomcat", "/etc/init.d/tomcat")
    #put("", "/etc/apache2/workers.properties")
    #put("", "/etc/apache2/conf.d/jk.conf")
    put("%(target)s/tomcat/install-tomcat.sh"%env, env.mgr_work_tomcat)
    with cd(env.mgr_work_tomcat):
        sudo("bash install-tomcat.sh %(tomcat_version)s ."%env)
    #sudo("chown root:root /etc/apache2/conf.d/jk.conf")
    #$ /etc/init.d/apache2 stop
    #$ /etc/init.d/tomcat restart
    #$ /etc/init.d/apache2 start

