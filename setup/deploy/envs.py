from fabric.api import *
import sys
x = lambda: dict(env, **sys._getframe(1).f_locals)

##
# Deployment environments

def dev_unix():
    # Name env:
    env.deployenv = 'dev-unix'
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

def integration():
    # Name env:
    env.deployenv = 'integration'
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

def staging():
    # Name env:
    env.deployenv = 'stg'
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

def production():
    # Name env:
    env.deployenv = 'prod'
    raise NotImplementedError

deployenvs = [dev_unix, integration, staging, production]

def _needs_deployenv():
    require('deployenv', 'roledefs', 'dist_dir', 'tomcat',
            provided_by=deployenvs)

##
# Instrumental commands and functions

@runs_once
def install_rinfo_pkg():
    local("cd %(java_packages)s/ && mvn install"%x(), capture=False)
    # TODO:? This also "installs" final war:s etc.. Use mvn-param for install dest.?

def deploy_war(localwar, warname):
    _needs_deployenv()
    put(localwar, "%(dist_dir)s/%(warname)s.war"%x())
    sudo("%(tomcat_stop)s"%x())
    sudo("rm -rf %(tomcat_webapps)s/%(warname)s/"%x())
    sudo("mv %(dist_dir)s/%(warname)s.war %(tomcat_webapps)s/"%x())
    sudo("%(tomcat_start)s"%x())

##
# Shared diagnostics

def list_dist():
    _needs_deployenv()
    run("ls -latr %(dist_dir)s/"%x())

def clean_dist():
    _needs_deployenv()
    run("rm -rf %(dist_dir)s/*"%x())

def tail():
    _needs_deployenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f"%x())

def restart():
    _needs_deployenv()
    sudo("%(tomcat_stop)s"%x())
    sudo("%(tomcat_start)s"%x())

def restart_apache():
    _needs_deployenv()
    # .. apache2ctl
    sudo("/etc/init.d/apache2 stop")
    sudo("/etc/init.d/apache2 start")

def war_props(warname="ROOT"):
    _needs_deployenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war WEB-INF/classes/*.properties"%x())

##
# Runtime operations

def ping_main_collector():
    #require('roledefs', provided_by=deployenvs)
    collector_url = "http://%s/collector/" % env.roledefs['main'][0]
    feed_url = "http://%s:8182/feed/current" % env.roledefs['examples'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%x())

def ping_service_collector():
    #require('roledefs', provided_by=deployenvs)
    collector_url = "http://%s/collector/" % env.roledefs['service'][0]
    feed_url = "http://%s/feed/current" % env.roledefs['main'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%x())

# Hack to tell fabric these are nocommand:s
from fabric.main import _internals
_internals += [x, deploy_war]

