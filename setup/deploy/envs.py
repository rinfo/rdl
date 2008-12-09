
##
# Environments

def dev_unix():
    # Name env:
    config.env = 'dev-unix'
    # 
    config(
        tomcat="/usr/share/tomcat6",
        tomat_webapps="$(tomcat)/webapps",
    )
    # Machines:    
    config(
        host_map={
            'localhost': ['127.0.0.1'],
        },
    )
    config.fab_hosts = config.host_map['localhost']
    

def staging():
    # Name env:
    config.env = 'stg'
    # SuSE layout:
    config(
        tomcat="/usr/share/tomcat6",
        tomat_webapps="$(tomcat)/webapps",
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
        dist_dir='rinfo_dist',
        rinfo_dir='/opt/rinfo',
    )

def production():
    # Name env:
    config.env = 'prod'
    # TODO:
    raise NotImplementedError

##
# Service targets

_needs_env = requires(
        'host_map', 'dist_dir', 'tomcat',
        provided_by=[staging, production])

@_needs_env
def main():
    config.fab_hosts = config.host_map['main']
    config.app_name = 'rinfo-main'
    config.rinfo_main_store = "/opt/rinfo/store"

@_needs_env
def service():
    config.fab_hosts = config.host_map['service']
    config.app_name = 'rinfo-service'

@_needs_env
def testsources():
    config.fab_hosts = config.host_map['testsources']
    config.app_name = 'rinfo-depot'
    config.test_store = "/opt/testsources/depots"

##
# Shared diagnostics

_needs_target = requires(
        'fab_hosts', 'dist_dir', 'tomcat', 'app_name',
        provided_by=[main, service, testsources])

@_needs_target
def list_dist(ls=""):
    if ls: ls = "-"+ls
    run("ls %s $(dist_dir)/" % ls)

@_needs_target
def clean_dist():
    run("rm $(dist_dir)/*", fail='warn')

@_needs_target
def tail():
    sudo("tail -f $(tomcat)/logs/catalina.out")

@_needs_target
def restart():
    sudo("$(tomcat_stop)", fail='warn')
    sudo("$(tomcat_start)")

@_needs_target
def war_props(war="ROOT"):
    run("unzip -p $(tomat_webapps)/%s.war "
            "WEB-INF/classes/$(app_name).properties" % war)

