from __future__ import with_statement
from fabric.api import *
from fabric.contrib.files import exists
from util import venv
from deploy import local_lib_rinfo_pkg, _deploy_war
from targetenvs import _needs_targetenv

##
# Local build

@runs_once
def package_service(deps="1"):
    if int(deps): local_lib_rinfo_pkg()
    _needs_targetenv()
    local("cd %(java_packages)s/rinfo-service/ && "
            "mvn -P%(target)s clean package war:war"%env, capture=False)

##
# Server deploy

@runs_once
@roles('service')
def setup_service():
    _needs_targetenv()
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    if not exists(env.rinfo_dir):
        sudo("mkdir %(rinfo_dir)s"%env)
    if not exists(env.rinfo_rdf_repo_dir):
        sudo("mkdir %(rinfo_rdf_repo_dir)s"%env)
    sudo("chown %(tomcat_user)s %(rinfo_rdf_repo_dir)s"%env)

@roles('service')
def deploy_service(headless="0"):
    setup_service()
    _deploy_war(
            "%(java_packages)s/rinfo-service/target/rinfo-service-%(target)s.war"%env,
            "rinfo-service", int(headless))

@roles('service')
def service_all(deps="1", headless="0"):
    package_service(deps)
    deploy_service(headless)

##
# Sesame and Repo Util deploy

@runs_once
@roles('service')
def package_sesame():
    env.pkgdir = "%(java_packages)s/rinfo-sesame-http"%env
    local("cd %(pkgdir)s && mvn package"%env)
    env.local_sesame_dir = "%(pkgdir)s/target/dependency"%env

@runs_once
@roles('service')
def deploy_sesame():
    setup_service()
    package_sesame()
    _patch_catalina_properties()
    for warname in ['openrdf-sesame', 'sesame-workbench']:
        _deploy_war("%(local_sesame_dir)s/%(warname)s.war"%venv(), warname)

def _patch_catalina_properties():
    # This will patch catalina.properties so that it contains the system 
    # property that controls where sesame stores its data
    sesame_data_dir_key = "info.aduna.platform.appdata.basedir"
    catalina_properties_path = "%(tomcat)s/conf/catalina.properties" % env
    with settings(warn_only=True):
        if (sudo("grep '%(sesame_data_dir_key)s' %(catalina_properties_path)s" % venv())):
            print "'%(sesame_data_dir_key)s' already present in %(catalina_properties_path)s" % venv()
        else:
            print "'%(sesame_data_dir_key)s' NOT found in %(catalina_properties_path)s" % venv()
            print "Patching %(catalina_properties_path)s" % venv()
            sudo("echo '# The data dir for Sesame used by rinfo-service' >> %(catalina_properties_path)s" % venv())
            sudo("echo '%(sesame_data_dir_key)s=%(rinfo_rdf_repo_dir)s' >> %(catalina_properties_path)s" % venv())

##
# Manage repository

@roles('service')
def service_repotool():
    setup_service()
    env.rinfo_repo_jar = "rinfo-rdf-repo-%s-jar-with-dependencies.jar" % env.java_pkg_version
    env.rinfo_service_props = "rinfo-service.properties"

@runs_once
@roles('service')
def deploy_repotool():
    service_repotool()
    local("cd %(java_packages)s/rinfo-rdf-repo && "
            "mvn -P %(target)s assembly:assembly"%env)
    put("%(java_packages)s/rinfo-service/src/environments/%(target)s/%(rinfo_service_props)s"%env,
            "%(dist_dir)s/"%env)
    put("%(java_packages)s/rinfo-rdf-repo/target/%(rinfo_repo_jar)s"%env, "%(dist_dir)s/"%env)

@roles('service')
def setup_repo():
    _repotool('setup')

@roles('service')
def clean_repo():
    _repotool('clean')

def _repotool(cmd):
    service_repotool()
    run("cd %(dist_dir)s && "
            "java -jar %(rinfo_repo_jar)s %(cmd)s "
            "%(rinfo_service_props)s rinfo.service.repo"%venv())

