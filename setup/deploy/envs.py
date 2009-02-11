
##
# System environments

def dev_unix():
    # Name env:
    config.env = 'dev-unix'
    #
    config(
        tomcat="/usr/share/tomcat6",
        tomcat_webapps="$(tomcat)/webapps",
        tomcat_start='$(tomcat)/bin/catalina.sh start',
        tomcat_stop='$(tomcat)/bin/catalina.sh stop',
    )
    # Machines:
    config(
        host_map={
            'main': ['localhost'],
            'service': ['localhost'],
            'testsources': ['localhost'],
        },
        store_map={
            'main': "/opt/_workapps/rinfo/depots/rinfo",
            'testsources': "/opt/_workapps/rinfo/depots",
        },
        dist_dir='/opt/_workapps/rinfo/rinfo_dist',
        rinfo_dir='/opt/_workapps/rinfo',
        rinfo_rdf_repo_dir='/opt/_workapps/rinfo/aduna',
    )


def virt_test():
    # Name env:
    config.env = 'virt-test'
    # Ubuntu layout:
    config(
        tomcat="/var/lib/tomcat6",
        tomcat_webapps="$(tomcat)/webapps",
        tomcat_start='/etc/init.d/tomcat6 start',
        tomcat_stop='/etc/init.d/tomcat6 stop',
    )
    # Machines:
    config(
        host_map={
            'main': ['rinfo-main'],
            'service': ['rinfo-service'],
            'testsources': ['rinfo-sources'],
        },
        store_map={
            'main': "/opt/rinfo/store",
        },
        dist_dir='rinfo_dist',
        rinfo_dir='/opt/rinfo',
        rinfo_rdf_repo_dir='/opt/rinfo/rdf',
    )

def staging():
    # Name env:
    config.env = 'stg'
    # SuSE layout:
    config(
        tomcat="/usr/share/tomcat6",
        tomcat_webapps="$(tomcat)/webapps",
        tomcat_start='dtomcat6 start',
        tomcat_stop='dtomcat6 stop',
    )
    # Machines:
    config(
        fab_user='rinfo',
        host_map={
            'main': ['rinfo-main.statskontoret.se'],
            'service': ['rinfo-service.statskontoret.se'],
            'testsources': ['rinfo-sources.statskontoret.se'],
        },
        store_map={
            'main': "/opt/rinfo/store",
            'testsources': "/opt/testsources/depots",
        },
        dist_dir='rinfo_dist',
        rinfo_dir='/opt/rinfo', # TODO: remove if base is packaged in
        rinfo_rdf_repo_dir='/opt/rinfo/rdf',
    )

def production():
    # Name env:
    config.env = 'prod'
    raise NotImplementedError

sysenvs = [dev_unix, virt_test, staging, production]


##
# Service targets

_needs_sysenv = requires('host_map', 'dist_dir', 'tomcat',
        provided_by=sysenvs)

@_needs_sysenv
def main():
    config.fab_hosts = config.host_map['main']
    config.rinfo_main_store = config.store_map['main']
    config.app_name = 'rinfo-main'

@_needs_sysenv
def service():
    config.fab_hosts = config.host_map['service']
    config.app_name = 'rinfo-service'

@_needs_sysenv
def testsources():
    config.fab_hosts = config.host_map['testsources']
    config.test_store = config.store_map['testsources'] #+ "/example.org"
    config.app_name = 'rinfo-depot'

_needs_target = requires('fab_hosts', 'dist_dir', 'tomcat', 'app_name',
        provided_by=[main, service, testsources])

##
# Instrumental commands

@_needs_target
def deploy_war(localwar, warname):
    l = vars()
    put(localwar, '$(dist_dir)/%(warname)s.war' % l)
    sudo("$(tomcat_stop)", fail='warn')
    sudo("rm -rf $(tomcat_webapps)/%(warname)s/" % l)
    sudo("mv $(dist_dir)/%(warname)s.war $(tomcat_webapps)/" % l)
    sudo("$(tomcat_start)")

##
# Shared diagnostics

@_needs_target
def list_dist(ls=""):
    if ls: ls = "-"+ls
    run("ls %s $(dist_dir)/" % ls)

@_needs_target
def clean_dist():
    run("rm -rf $(dist_dir)/*", fail='warn')

@_needs_target
def tail():
    sudo("tail -f $(tomcat)/logs/catalina.out")

@_needs_target
def restart():
    sudo("$(tomcat_stop)", fail='warn')
    sudo("$(tomcat_start)")

@_needs_target
def war_props(war="ROOT"):
    run("unzip -p $(tomcat_webapps)/%s.war "
            "WEB-INF/classes/$(app_name).properties" % war)

