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
        'main': ['demo.lagrummet.se'],
        'service': ['demo.lagrummet.se'],
        'checker': ['demo.lagrummet.se'],
        'admin': ['demo.lagrummet.se'],
        'demosource': ['demo.lagrummet.se'],
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
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
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
    env.tomcat_version = "7.0.47"
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = '/etc/init.d/tomcat start'
    env.tomcat_stop = '/etc/init.d/tomcat stop'
    env.tomcat_user = 'tomcat'
    env.tomcat_group = 'tomcat'

