from __future__ import with_statement
from fabric.api import env, local, cd
from fabric.contrib.files import *
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from deploy import _deploy_war
from util import venv

env.java_opts = 'JAVA_OPTS="-Xms512Mb -Xmx1024Mb"'
env.test_data_dir = "/opt/_workapps/rinfo/testdata"
env.test_data_tools = "../tools/testscenarios"

lagen_nu_datasets = ('sfs', 'dv')
riksdagen_se_datasets = ('prop', 'sou', 'ds')

def demo_data_download(dataset):
    """Downloads a demo dataset from its source"""
    _mkdir_keep_prev("%(test_data_dir)s/%(dataset)s-raw"%venv())

    with cd("%(test_data_dir)s/%(dataset)s-raw"%venv()):
        if dataset in lagen_nu_datasets:
            _download_lagen_nu_data(dataset)
        else if dataset in riksdagen_se_datasets:
            raise NotImplementedError
        else:
            raise ValueError("Dataset '%s' is not known to me"%dataset)

def demo_data_to_depot(dataset):
    """Transforms the downloaded demo data to a depot"""
    _mkdir_keep_prev("%(test_data_dir)s/%(dataset)s"%venv())

    if dataset in lagen_nu_datasets:
        _transform_lagen_nu_data(dataset)
    else if dataset in riksdagen_se_datasets:
        raise NotImplementedError
    else:
        raise ValueError("Dataset '%s' is not known to me"%dataset)

def demo_data_upload(dataset):
    """Uploads the transformed demo data depot to the demo server"""
    _needs_targetenv()
    rsync_project((env.demo_data_root), "%(test_data_dir)s/%(dataset)s"%venv(), exclude=".*", delete=True)

def demo_build_war(dataset):
    """Builds a webapp capable of serving an uploaded demo data depot"""
    local("cd %(java_packages)s/demodata-supply && "
            "mvn -Ddataset=%(dataset)s -Ddemodata-root=%(demo_data_root)s clean package"%venv(), capture=False)

def demo_deploy_war(dataset):
    """Deploys an uploaded demo webapp"""
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    _deploy_war("%(java_packages)s/demodata-supply/target/%(dataset)s-demodata-supply.war"%venv(),
            "%(dataset)s-demodata-supply"%venv())

def demo_refresh(dataset):
    """Downloads, transforms, uploads, builds and deploys a webapp for serving a demo dataset"""
    demo_data_download(dataset)
    demo_data_to_depot(dataset)
    demo_data_upload(dataset)
    demo_build_war(dataset)
    demo_deploy_war(dataset)


#def serve_depot(depot=None, port=8180):
#    depot = depot or "%s/lagen-nu/%s" % (env.test_data_dir, "sfs")
#    local("groovy %s/serve_depot.groovy %s 8180" % (env.test_data_tools, depot))
#     # Ping service:
#    local('curl --data "feed=http://localhost:8180/feed/current" http://localhost:8181/collector')

def _mkdir_keep_prev(dir_path):
    if exists("%s-prev"%dir_path):
        local("rm -rf %s-prev"%dir_path)

    if exists("%s"%dir_path):
        local("mv %s %s-prev"%(dir_path, dir_path))

    local("mkdir -p %s"%dir_path)

def _download_lagen_nu_data(dataset):
    local("curl https://lagen.nu/%s/parsed/rdf.nt -o lagennu-%s.nt"%(dataset,dataset))

def _transform_lagen_nu_data(dataset):
    local("%(java_opts)s groovy %(test_data_tools)s/n3dump_to_depot.groovy %(test_data_dir)s/%(dataset)s-raw/lagennu-%(dataset)s.nt %(test_data_dir)s/sfs"%venv())