from __future__ import with_statement
from fabric.api import *
from fabric.contrib.files import exists as _exists
from deploy.envs import *

##
# Local build

@runs_once
def package_service(deps="1"):
    if int(deps): install_rinfo_pkg()
    require('deployenv', provided_by=deployenvs)
    local("cd %(java_packages)s/rinfo-service/ && "
            "mvn -P%(deployenv)s clean war:war"%env, capture=False)

##
# Server deploy

@runs_once
@roles('service')
def setup_service():
    require('dist_dir', 'rinfo_dir', 'rinfo_rdf_repo_dir', provided_by=deployenvs)
    if not _exists(env.dist_dir): run("mkdir %(dist_dir)s"%env)
    if not _exists(env.rinfo_dir): sudo("mkdir %(rinfo_dir)s"%env)
    if not _exists(env.rinfo_rdf_repo_dir):
        sudo("mkdir %(rinfo_rdf_repo_dir)s"%env)

@runs_once
@roles('service')
def deploy_service():
    setup_service()
    deploy_war(
            "%(java_packages)s/rinfo-service/target/rinfo-service-%(deployenv)s.war"%env,
            "rinfo-service")

@roles('service')
def service_all(deps="1"):
    package_service(deps)
    deploy_service()

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
    for warname in ['openrdf-sesame', 'sesame-workbench']:
        deploy_war("%(local_sesame_dir)s/%(warname)s.war"%env, warname)

##
# Manage repository

@roles('service')
def service_repotool():
    setup_service()
    env.rinfo_repo_jar = "rinfo-rdf-repo-1.0-SNAPSHOT-jar-with-dependencies.jar"
    env.rinfo_service_props = "rinfo-service.properties"

@runs_once
@roles('service')
def deploy_repotool():
    service_repotool()
    local("cd %(java_packages)s/rinfo-rdf-repo && "
            "mvn -P %(deployenv)s assembly:assembly"%env)
    put("%(java_packages)s/rinfo-service/src/environments/%(deployenv)s/%(rinfo_service_props)s"%env,
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
            "%(rinfo_service_props)s rinfo.service.repo"%x())

