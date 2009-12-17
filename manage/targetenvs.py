from fabric.api import *
from util import x

##
# Target environments

_fdoc_target = lambda f: f.__doc__.split(': ')[-1]

def tg_dev_unix():
    "Set target env to: dev-unix"
    # Name env:
    env.target = _fdoc_target(tg_dev_unix)
    # Tomcat
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps"%x()
    env.tomcat_start = "%(tomcat)s/bin/catalina.sh start"%x()
    env.tomcat_stop = "%(tomcat)s/bin/catalina.sh stop"%x()
    env.tomcat_user = "tomcat"
    # Machines:
    env.roledefs = {
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

def tg_integration():
    "Set target env to: integration"
    # Name env:
    env.target = _fdoc_target(tg_integration)
    # Tomcat (Ubuntu)
    env.tomcat = "/var/lib/tomcat6"
    env.tomcat_webapps = "%(tomcat)s/webapps"%x()
    env.tomcat_start = '/etc/init.d/tomcat6 start'
    env.tomcat_stop = '/etc/init.d/tomcat6 stop'
    env.tomcat_user = 'tomcat6'
    # Machines:
    env.roledefs = {
        'main': ['rinfo-main'],
        'service': ['rinfo-service'],
        'examples': ['rinfo-sources'],
    }
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.example_stores = "/opt/rinfo/depots"
    env.dist_dir = 'rinfo_dist'
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'

def tg_stg():
    "Set target env to: stg"
    # Name env:
    env.target = _fdoc_target(tg_stg)
    # Tomcat (SuSE):
    env.tomcat = "/usr/share/tomcat6"
    env.tomcat_webapps = "%(tomcat)s/webapps"%x()
    env.tomcat_start = 'dtomcat6 start'
    env.tomcat_stop = 'dtomcat6 stop'
    env.tomcat_user = 'tomcat6'
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

def tg_prod():
    "Set target env to: prod"
    # Name env:
    env.target = _fdoc_target(tg_prod)
    env.roledefs = {
        'main': ['94.247.169.66'],
        'service': ['94.247.169.67'],
    }
    # TODO: see above
    env.mgr_workdir = "mgr_work"
    env.mgr_work_tomcat = "%(mgr_workdir)s/tomcat" % env
    env.tomcat_version = "6.0.20"


targetenvs = [tg_dev_unix, tg_integration, tg_stg, tg_prod]

def _needs_deployenv():
    require('target', 'roledefs', 'dist_dir', 'tomcat',
            provided_by=targetenvs)

