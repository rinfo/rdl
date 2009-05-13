from fabric.api import *
from fabric.contrib.files import exists
from fmt import fmt
from envs import *
from envs import _deploy_war

##
# Local build

def package_service(deps="1"):
    if int(deps): install_rinfo_pkg()
    require('deployenv', provided_by=deployenvs)
    local(fmt("cd ${java_packages}/rinfo-service/; mvn -P${deployenv} clean package"))

##
# Server deploy

def setup_service():
    service()
    require('dist_dir', 'rinfo_dir', 'rinfo_rdf_repo_dir', provided_by=deployenvs)
    if not exists(env.dist_dir): run(fmt("mkdir $dist_dir"))
    if not exists(env.rinfo_dir): sudo(fmt("mkdir $rinfo_dir"))
    if not exists(env.rinfo_rdf_repo_dir):
        sudo(fmt("mkdir ${rinfo_rdf_repo_dir}"))

def deploy_service():
    setup_service()
    _deploy_war(
            fmt("${java_packages}/rinfo-service/target/rinfo-service-${deployenv}.war"),
            "rinfo-service")

def service_all():
    package_service()
    deploy_service()

##
# Sesame and Repo Util deploy

def package_sesame():
    pkgdir = fmt("${java_packages}/rinfo-sesame-http")
    local(fmt("cd ${pkgdir} && mvn package"))
    config.local_sesame_dir = fmt("${pkgdir}/target/dependency")

def deploy_sesame():
    setup_service()
    package_sesame()
    for warname in ['openrdf-sesame', 'sesame-workbench']:
        _deploy_war(fmt("${local_sesame_dir}/${warname}.war"), warname)

def service_repo_util():
    setup_service()
    config.rinfo_repo_jar = "rinfo-rdf-repo-1.0-SNAPSHOT-jar-with-dependencies.jar"
    config.rinfo_service_props = "rinfo-service.properties"

def deploy_repo_util():
    service_repo_util()
    local(fmt("cd ${java_packages}/rinfo-rdf-repo; mvn -P ${deployenv} assembly:assembly"))
    put(fmt("${java_packages}/rinfo-service/src/environments/${deployenv}/${rinfo_service_props}"),
            fmt("${dist_dir}/"))
    put(fmt("${java_packages}/rinfo-rdf-repo/target/${rinfo_repo_jar}"), "${dist_dir}/")

##
# Manage repository

def setup_repo():
    service_repo_util()
    run(fmt("cd ${dist_dir}; java -jar ${rinfo_repo_jar} setup ${rinfo_service_props} rinfo.service.repo"))

def clean_repo():
    service_repo_util()
    run(fmt("cd ${dist_dir}; java -jar ${rinfo_repo_jar} setup ${rinfo_service_props} rinfo.service.repo"))

