from __future__ import with_statement
from fabric.api import *
from fabric.contrib.files import exists
from fabfile.util import venv, mkdirpath
from fabfile import app, sysconf
from fabfile.target import _needs_targetenv

##
# Local build

@task
@runs_once
def package(deps="1", test="1"):
    """Builds and packages the rinfo-service war, configured for the target env."""
    if int(deps): app.local_lib_rinfo_pkg()
    _needs_targetenv()
    flags = "" if int(test) else "-Dmaven.test.skip=true"
    local("cd %(java_packages)s/rinfo-service/ && "
            "mvn %(flags)s -P%(target)s clean package war:war" % venv(), capture=False)

##
# Server deploy

@task
@runs_once
@roles('service')
def setup():
    """Creates neccessary directories for rinfo-service runtime data."""
    _needs_targetenv()
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    if not exists(env.rinfo_dir):
        sudo("mkdir %(rinfo_dir)s"%env)
    if not exists(env.rinfo_rdf_repo_dir):
        sudo("mkdir %(rinfo_rdf_repo_dir)s"%env)
        sudo("chown %(tomcat_user)s %(rinfo_rdf_repo_dir)s"%env)

@task
@roles('service')
def deploy(headless="0"):
    """Deploys the rinfo-service war package to target env."""
    setup()
    app._deploy_war(
            "%(java_packages)s/rinfo-service/target/rinfo-service-%(target)s.war"%env,
            "rinfo-service", int(headless))

@task
@roles('service')
def all(deps="1", test="1", headless="0"):
    """Packages and deploys the rinfo-service war to target env."""
    package(deps, test)
    deploy(headless)

##
# Sesame and Repo Util deploy

@task
@roles('service')
def package_sesame():
    """Packages and deploys the Sesame RDF store war to target env."""
    env.pkgdir = "%(java_packages)s/rinfo-sesame-http"%env
    local("cd %(pkgdir)s && mvn package"%env)
    env.local_sesame_dir = "%(pkgdir)s/target/dependency"%env

@task
@roles('service')
def deploy_sesame():
    setup()
    package_sesame()
    _patch_catalina_properties()
    for warname in ['openrdf-sesame', 'sesame-workbench']:
        app._deploy_war("%(local_sesame_dir)s/%(warname)s.war"%venv(), warname)

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

@task
@roles('service')
def repotool():
    setup()
    env.rinfo_repo_jar = "rinfo-rdf-repo-%s-jar-with-dependencies.jar" % env.java_pkg_version
    env.rinfo_service_props = "rinfo-service.properties"

@task
@runs_once
@roles('service')
def deploy_repotool():
    repotool()
    local("cd %(java_packages)s/rinfo-rdf-repo && "
            "mvn -P %(target)s assembly:assembly"%env)
    put("%(java_packages)s/rinfo-service/src/environments/%(target)s/%(rinfo_service_props)s"%env,
            "%(dist_dir)s/"%env)
    put("%(java_packages)s/rinfo-rdf-repo/target/%(rinfo_repo_jar)s"%env, "%(dist_dir)s/"%env)

@task
@roles('service')
def setup_repo():
    _repotool('setup')

@task
@roles('service')
def clean_repo():
    _repotool('clean')

def _repotool(cmd):
    repotool()
    run("cd %(dist_dir)s && "
            "java -jar %(rinfo_repo_jar)s %(cmd)s "
            "%(rinfo_service_props)s rinfo.service.repo"%venv())

##
# ElasticSearch install and setup

@task
@roles('service')
def install_elasticsearch():
    sysconf._sync_workdir()
    version, dist = fetch_elasticsearch()
    with cd("/opt/"):
        sudo("tar xzf %(dist)s" % vars())
        if exists("elasticsearch"):
            sudo("rm elasticsearch")
        sudo("ln -s elasticsearch-%(version)s elasticsearch" % vars())
        sysconf.install_init_d("elasticsearch")

def fetch_elasticsearch():
    elastic_version = "0.17.6"
    workdir_elastic = "%(mgr_workdir)s/elastic_pkg" % env
    mkdirpath(workdir_elastic)
    elastic_distfile = "elasticsearch-%(elastic_version)s.tar.gz" % vars()
    with cd(workdir_elastic):
        if not exists(elastic_distfile):
            run("wget https://github.com/downloads/elasticsearch/elasticsearch/%s" % elastic_distfile)
    return elastic_version, "%(workdir_elastic)s/%(elastic_distfile)s" % vars()

@task
@roles('service')
def stop_elasticsearch():
    _needs_targetenv()
    sudo("/etc/init.d/elasticsearch stop")

@task
@roles('service')
def start_elasticsearch():
    _needs_targetenv()
    sudo("/etc/init.d/elasticsearch start")

