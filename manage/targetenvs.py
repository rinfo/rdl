"""
Target Environments
"""
from fabric.api import *


targetenvs = []

def _targetenv(f):
    """
    Decorator function that makes sure that the list targetenvs contains all 
    available target environments. It does so by adding the decorated function 
    to the targetenvs list which is used by the _needs_targetenv function. 
    """
    targetenvs.append(f)
    return f

def _needs_targetenv():
    """
    Makes sure that the env dictionary contains a certain set of keys. These 
    keys are provided by one of the targetenv functions (decorated with 
    @_targetenv). Targets calling this function i.e. 'package_main' requires 
    a target and are executed like this:
    
        fab <some_targetenv_target> package_main
    """
    require('target', 'roledefs', 'dist_dir', 'tomcat', provided_by=targetenvs)

@_targetenv
def tg_dev_unix():
    """Set target env to: dev-unix"""
    # Name env:
    env.target = "dev-unix"
    # Machines:
    env.roledefs = {
        'admin': ['localhost'],
        'main': ['localhost'],
        'service': ['localhost'],
        'examples': ['localhost'],
    }
    # Filesystem paths
    env.rinfo_main_store = "/opt/_workapps/rinfo/depots/rinfo"
    env.examples_store = "/opt/_workapps/rinfo/depots"
    env.dist_dir = '/opt/_workapps/rinfo/rinfo_dist'
    env.rinfo_dir = '/opt/_workapps/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/_workapps/rinfo/aduna'
    env.admin_webroot = "/opt/_workapps/rinfo/admin"
    # Tomcat
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = "%(tomcat)s/bin/catalina.sh start"%env
    env.tomcat_stop = "%(tomcat)s/bin/catalina.sh stop"%env
    env.tomcat_user = "tomcat"

@_targetenv
def tg_integration():
    """Set target env to: integration"""
    # Name env:
    env.target = "integration"
    # Machines:
    env.roledefs = {
        'main': ['rinfo-main'],
        'service': ['rinfo-service'],
        #'examples': ['rinfo-sources'],
        'doc': ['rinfo-main'],
    }
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.example_stores = "/opt/rinfo/depots"
    env.dist_dir = 'rinfo_dist'
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'
    env.docs_webroot = "/var/www/dokumentation/"
    # Tomcat (Ubuntu)
    env.tomcat = "/var/lib/tomcat6"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = '/etc/init.d/tomcat6 start'
    env.tomcat_stop = '/etc/init.d/tomcat6 stop'
    env.tomcat_user = 'tomcat6'

@_targetenv
def tg_stg():
    """Set target env to: stg"""
    # Name env:
    env.target = "stg"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo-main.statskontoret.se'],
        'service': ['rinfo-service.statskontoret.se'],
        'examples': ['rinfo-sources.statskontoret.se'],
    }
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.example_stores = "/opt/rinfo/depots"
    env.dist_dir = 'rinfo_dist'
    env.rinfo_dir = '/opt/rinfo' # TODO: remove if base is packaged in
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'
    # Tomcat (SuSE):
    env.tomcat = "/usr/share/tomcat6"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = 'dtomcat6 start'
    env.tomcat_stop = 'dtomcat6 stop'
    env.tomcat_user = 'tomcat6'

@_targetenv
def tg_prod():
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
    # System paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['default', 'admin'],
        'service': ['service', 'checker'],
    }
    env.apache_jk_tomcat = True
    # Tomcat
    env.custom_tomcat = True
    env.tomcat_version = "6.0.20"
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps"%env
    env.tomcat_start = '/etc/init.d/tomcat start'
    env.tomcat_stop = '/etc/init.d/tomcat stop'
    env.tomcat_user = 'tomcat'

