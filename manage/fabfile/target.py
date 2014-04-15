"""
Target Environments
"""
from fabric.api import *


targetenvs = []

def targetenv(f):
    """
    Decorator function that makes sure that the list targetenvs contains all
    available target environments. It does so by adding the decorated function
    to the targetenvs list which is used by the _needs_targetenv function.
    """
    targetenvs.append("target." + f.__name__)
    return task(f)

def _needs_targetenv():
    """
    Makes sure that the env dictionary contains a certain set of keys. These
    keys are provided by one of the targetenv functions (decorated with
    @targetenv). Targets calling this function require a target to have been
    invoked.
    """
    require('target', 'roledefs', 'dist_dir', 'tomcat', provided_by=targetenvs)

@targetenv
def dev_unix():
    """Set target env to: dev_unix"""
    # Name env:
    env.target = "dev_unix"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['localhost'],
        'service': ['localhost'],
        'checker': ['localhost'],
        'admin': ['localhost'],
        'demosource': ['localhost'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()


@targetenv
def demo():
    """Set target env to: demo"""
    # Name env:
    env.target = "demo"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.demo.lagrummet.se'],
        'service': ['service.demo.lagrummet.se'],
        'checker': ['checker.demo.lagrummet.se'],
        'admin': ['admin.demo.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
        'lagrummet': ['demo.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def test():
    """Set target env to: test"""
    # Name env:
    env.target = "test"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.test.lagrummet.se'],
        'service': ['service.test.lagrummet.se'],
        'checker': ['checker.test.lagrummet.se'],
        'admin': ['admin.test.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
        'lagrummet': ['test.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def regression():
    """Set target env to: regression"""
    # Name env:
    env.target = "regression"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.regression.lagrummet.se'],
        'service': ['service.regression.lagrummet.se'],
        'checker': ['checker.regression.lagrummet.se'],
        'admin': ['admin.regression.lagrummet.se'],
        'demosource': ['regression.testfeed.lagrummet.se'],
        'lagrummet': ['regression.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def beta():
    """Set target env to: beta"""
    # Name env:
    env.target = "beta"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.beta.lagrummet.se'],
        'service': ['service.beta.lagrummet.se'],
        'checker': ['checker.beta.lagrummet.se'],
        'admin': ['admin.beta.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
        'lagrummet': ['beta.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish, if installing distributed make sure listen_ip_varnish is empty (listen to all interfaces)
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = ""
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def skrapat():
    """Set target env to: Skrapat"""
    # Name env:
    env.target = "skrapat"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.skrapat.lagrummet.se'],
        'service': ['service.skrapat.lagrummet.se'],
        'checker': ['checker.skrapat.lagrummet.se'],
        'admin': ['admin.skrapat.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
        'lagrummet': ['skrapat.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()


@targetenv
def testfeed():
    """Set target env to: env

       To work, you must set correct host(env.roledefs) values in /etc/hosts.
    """
    # Name env:
    env.target = "testfeed"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['main.testfeed'],
        'service': ['service.testfeed'],
        'checker': ['checker.testfeed'],
        'admin': ['admin.testfeed'],
        'demosource': ['testfeed.lagrummet.se'],
        'regression': ['regression.testfeed.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['emfs'],
        'regression': ['regression'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def scraped():
    """Set target env to: demo"""
    # Name env:
    env.target = "scraped"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['testfeed.lagrummet.se'],
        'service': ['testfeed.lagrummet.se'],
        'checker': ['testfeed.lagrummet.se'],
        'admin': ['testfeed.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

# Integration is a virtual environment that you could setup on your own computer
# See README.txt for more information
@targetenv
def integration():
    """Set target env to: integration"""
    # Name env:
    env.target = "integration"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo-main'],
        'service': ['rinfo-service'],
        'checker': ['rinfo-checker'],
        'doc': ['rinfo-integration'],
        'admin': ['rinfo-integration'],
        'demosource': ['rinfo-integration'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

@targetenv
def prod():
    """Set target env to: prod"""
    # Name env:
    env.target = "prod"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.lagrummet.se'],
        'service': ['service.lagrummet.se'],
        'checker': ['checker.lagrummet.se'],
        'doc': ['dev.lagrummet.se'],
        'admin': ['admin.lagrummet.se'],
        'lagrummet': ['www.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = ""
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['default', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()

def _tomcat_env():
    env.apache_jk_tomcat = True
    # when change version of tomcat, must check server.xml (../../sysconf/common/tomcat/server.xml)
    env.tomcat_version = "7.0.53"
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = '/etc/init.d/tomcat start'
    env.tomcat_stop = '/etc/init.d/tomcat stop'
    env.tomcat_user = 'tomcat'
    env.tomcat_group = 'tomcat'

