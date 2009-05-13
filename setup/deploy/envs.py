from fabric.api import *
from fmt import fmt

##
# Deployment environments

def dev_unix():
    # Name env:
    env.deployenv = 'dev-unix'
    #
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = fmt("${tomcat}/webapps")
    env.tomcat_start = fmt("${tomcat}/bin/catalina.sh start")
    env.tomcat_stop = fmt("${tomcat}/bin/catalina.sh stop")
    env.tomcat_user = "tomcat"

    # Machines:
    env.host_map = {
        'main': ['localhost'],
        'service': ['localhost'],
        'testsources': ['localhost'],
    }
    env.store_map = {
        'main': "/opt/_workapps/rinfo/depots/rinfo",
        'testsources': "/opt/_workapps/rinfo/depots",
    }
    env.dist_dir = '/opt/_workapps/rinfo/rinfo_dist'
    env.rinfo_dir = '/opt/_workapps/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/_workapps/rinfo/aduna'


def integration():
    # Name env:
    env.deployenv = 'integration'
    # Ubuntu layout:
    env.tomcat = "/var/lib/tomcat6"
    env.tomcat_webapps = fmt("${tomcat}/webapps")
    env.tomcat_start = '/etc/init.d/tomcat6 start'
    env.tomcat_stop = '/etc/init.d/tomcat6 stop'
    env.tomcat_user = 'tomcat6'
    # Machines:
    env.host_map={
        'main': ['rinfo-main'],
        'service': ['rinfo-service'],
        'testsources': ['rinfo-sources'],
    }
    env.store_map={
        'main': "/opt/rinfo/store",
    }
    env.dist_dir = 'rinfo_dist'
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'

def staging():
    # Name env:
    env.deployenv = 'stg'
    # SuSE layout:
    env.tomcat = "/usr/share/tomcat6"
    env.tomcat_webapps = fmt("${tomcat}/webapps")
    env.tomcat_start = 'dtomcat6 start'
    env.tomcat_stop = 'dtomcat6 stop'
    env.tomcat_user = 'tomcat6'

    # Machines:
    env.user = 'rinfo'
    env.host_map = {
        'main': ['rinfo-main.statskontoret.se'],
        'service': ['rinfo-service.statskontoret.se'],
        'testsources': ['rinfo-sources.statskontoret.se'],
    }
    env.store_map = {
        'main': "/opt/rinfo/store",
        'testsources': "/opt/testsources/depots",
    }
    env.dist_dir = 'rinfo_dist'
    env.rinfo_dir = '/opt/rinfo' # TODO: remove if base is packaged in
    env.rinfo_rdf_repo_dir = '/opt/rinfo/rdf'

def production():
    # Name env:
    env.deployenv = 'prod'
    raise NotImplementedError

deployenvs = [dev_unix, integration, staging, production]


##
# Service roles

def _needs_deployenv():
    require('deployenv', 'host_map', 'dist_dir', 'tomcat',
            provided_by=deployenvs)

def main():
    _needs_deployenv()
    env.hosts = env.host_map['main']
    env.rinfo_main_store = env.store_map['main']
    env.app_name = 'rinfo-main'

def service():
    _needs_deployenv()
    env.hosts = env.host_map['service']
    env.app_name = 'rinfo-service'

def testsources():
    _needs_deployenv()
    env.hosts = env.host_map['testsources']
    env.test_store = env.store_map['testsources'] #+ "/example.org"
    env.app_name = 'rinfo-depot'


def _needs_role():
    require('hosts', 'dist_dir', 'tomcat', 'app_name',
            provided_by=[main, service, testsources])

##
# Instrumental commands

@runs_once
def install_rinfo_pkg():
    local(fmt("cd ${java_packages}/ && mvn install"))
    # TODO:? This also "installs" final war:s etc.. Use mvn-param for install dest.?

def _deploy_war(localwar, warname):
    _needs_role()
    put(localwar, fmt("${dist_dir}/${warname}.war"))
    sudo(fmt("${tomcat_stop}"))
    sudo(fmt("rm -rf ${tomcat_webapps}/${warname}/"))
    sudo(fmt("mv ${dist_dir}/${warname}.war ${tomcat_webapps}/"))
    sudo(fmt("${tomcat_start}"))

##
# Shared diagnostics

def list_dist(ls=""):
    _needs_role()
    if ls: ls = "-"+ls
    run(fmt("ls -latr ${ls} ${dist_dir}/"))

def clean_dist():
    _needs_role()
    run(fmt("rm -rf ${dist_dir}/*"))

def tail():
    _needs_role()
    sudo(fmt("ls -t ${tomcat}/logs/catalina*.* | head -1 | xargs tail -f"))

def restart():
    _needs_role()
    sudo(fmt("${tomcat_stop}"))
    sudo(fmt("${tomcat_start}"))

def restart_apache():
    _needs_role()
    # .. apache2ctl
    sudo("/etc/init.d/apache2 stop")
    sudo("/etc/init.d/apache2 start")

def war_props(war="ROOT"):
    _needs_role()
    run(fmt("unzip -p ${tomcat_webapps}/${war}.war WEB-INF/classes/${app_name}.properties"))

