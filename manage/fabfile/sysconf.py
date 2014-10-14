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
from fabfile.target import _needs_targetenv
from fabfile.util import mkdirpath


##
# Continuous Maintenance


@task
def configure_server():
    secure_sshd()
    _sync_workdir()
    sync_static_web()
    configure_app_container()
    configure_sites()


@runs_once
def _sync_workdir():
    for confdir in [p.join(env.manageroot, "sysconf", "common"),
                    p.join(env.manageroot, "sysconf", env.target)]:
        rsync_project(env.mgr_workdir, confdir, exclude=".*", delete=True)


@task
def sync_static_web():
    _sync_workdir()
    targetenv_www_dir = "%(mgr_workdir)s/%(target)s/www" % env
    with cd(targetenv_www_dir):
        for fname in ['index.html', 'robots.txt']:
            if not exists(fname):
                continue
            www = "/var/www"
            dest = "%s/%s" % (www, fname)
            if sudo("cp -vu %s %s/" % (fname, www)):
                sudo("chmod u=rw,a=r %s" % dest)


@task
def configure_app_container():
    _sync_workdir()
    with cd("%(mgr_workdir)s/common/etc" % env):
        if env.get('apache_jk_tomcat'):
            if sudo("cp -vu apache2/workers.properties /etc/apache2/"):
                sudo("chown root:root /etc/apache2/workers.properties")
            if sudo("cp -vu apache2/conf.d/jk.conf /etc/apache2/conf.d/"):
                sudo("chown root:root /etc/apache2/conf.d/jk.conf")


@task
def configure_sites():
    _sync_workdir()
    targetenv_etc_dir = "%(mgr_workdir)s/%(target)s/etc" % env
    with cd(targetenv_etc_dir):
        for role in env.roles:
            sites = env.get('apache_sites')
            if not sites or role not in sites:
                continue
            for site in sites[role]:
                sudo("cp -vu apache2/sites-available/%s /etc/apache2/sites-available/" % site)
                sudo("a2ensite %s" % site)


@task
def install_init_d(name):
    _sync_workdir()
    with cd("%(mgr_workdir)s/common/etc" % env):
        if sudo("cp -vu init.d/%s /etc/init.d/" % name):
            sudo("chmod 0755 /etc/init.d/%s" % name)
            sudo("update-rc.d %s defaults" % name)


##
# Initial Software Installation


@runs_once
def _prepare_mgr_work():
    _needs_targetenv()
    mkdirpath("%(mgr_workdir)s/install" % env)
    put(p.join(env.manageroot, "sysconf", "install", "*.sh"), "%(mgr_workdir)s/install" % env)
    put(p.join(env.manageroot, "sysconf", "common", "tomcat", "server.xml"), "%(mgr_workdir)s/install" % env)
    mkdirpath("%(mgr_workdir)s/tomcat_pkg" % env)


@task
def install_server():
    install_dependencies()
    #install_jdk() # Installing the Proprietary JDK requires manual confirmation.
    install_tomcat()


@task
def install_dependencies():
    _prepare_mgr_work()
    sudo("bash %(mgr_workdir)s/install/1_deps.sh" % env)


@task
def install_tomcat():
    fetch_tomcat_dist()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    with cd(workdir_tomcat):
        sudo("bash %(mgr_workdir)s/install/3_install-tomcat.sh %(tomcat_version)s %(tomcat_user)s "
             "%(tomcat_group)s %(user)s %(mgr_workdir)s" % env)
    install_init_d("tomcat")


def fetch_tomcat_dist():
    _prepare_mgr_work()
    workdir_tomcat = "%(mgr_workdir)s/tomcat_pkg" % env
    with cd(workdir_tomcat):
        run("bash %(mgr_workdir)s/install/2_get-tomcat.sh %(tomcat_version)s" % env)


def secure_sshd():
    sudo("sed -i 's/^#PermitRootLogin yes/PermitRootLogin no/;s/PermitRootLogin yes/PermitRootLogin no/;"
         "s/^#PermitEmptyPasswords yes/PermitEmptyPasswords no/;s/PermitEmptyPasswords yes/PermitEmptyPasswords no/;"
         "s/^#X11Forwarding yes/X11Forwarding no/;s/X11Forwarding yes/X11Forwarding no/' /etc/ssh/sshd_config")
    sudo("/etc/init.d/ssh restart")


@task
@roles('main', 'service', 'checker', 'admin', 'lagrummet', 'emfs', 'test', 'regression', 'skrapat', 'demosource')
def put_vhost_profile(app_name, local_lan=True):
    vhost_settings = read_template('sysconf/common/etc/apache2/apache_vhost_template.txt')
    if local_lan:
        vhost_settings = vhost_settings.replace('[%s]' % "app.vhost.dns.name", "%s.%s" % (app_name, env.target))
    else:
        vhost_settings = vhost_settings.replace('[%s]' % "app.vhost.dns.name", "%s.%s.lagrummet.se" % (app_name, env.target))
    if local_lan:
        vhost_settings = vhost_settings.replace('[%s]' % "app.ajp.url", "ajp://localhost:8009/rinfo-checker/" % (app_name, env.target))
    else:
        vhost_settings = vhost_settings.replace('[%s]' % "app.ajp.url", "ajp://%s.lagrummet.se:8009/rinfo-%s/" % (app_name, env.target))

    vhost_settings = vhost_settings.replace('[%s]' % "app.error_log.file_and_path_name", "/var/log/apache2/%s-error.log" % (app_name))
    vhost_settings = vhost_settings.replace('[%s]' % "app.access_log.file_and_path_name", "/var/log/apache2/%s-access.log combined" % (app_name))
    file_name = "%s_%s" % (app_name,env.target)
    write_to_apache_vhost_on_remote_server(file_name,vhost_settings)
