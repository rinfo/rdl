from __future__ import with_statement
from fabric.api import env, local, cd
from fabric.contrib.files import *
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from deploy import _deploy_war
from util import venv
from os import path as p


env.java_opts = 'JAVA_OPTS="-Xms512Mb -Xmx1024Mb"'
env.demodata_dir = "/opt/_workapps/rinfo/demodata"
env.demodata_tools = p.join(env.projectroot, "tools", "demodata")


lagen_nu_datasets = ('sfs', 'dv')
riksdagen_se_datasets = ('prop', 'sou', 'ds')

def _can_handle_dataset(dataset):
    if not any(dataset in ds for ds in (lagen_nu_datasets, riksdagen_se_datasets)):
        raise ValueError("Undefined dataset %r" % dataset)


def demo_data_download(dataset):
    """Downloads a demo dataset from its source."""
    _can_handle_dataset(dataset)
    _mkdir_keep_prev("%(demodata_dir)s/%(dataset)s-raw" % venv())
    with cd("%(demodata_dir)s/%(dataset)s-raw" % venv()):
        if dataset in lagen_nu_datasets:
            _download_lagen_nu_data(dataset)
        elif dataset in riksdagen_se_datasets:
            _download_riksdagen_data(dataset)

def demo_data_to_depot(dataset):
    """Transforms the downloaded demo data to a depot."""
    _can_handle_dataset(dataset)
    if dataset in lagen_nu_datasets:
        _transform_lagen_nu_data(dataset)
    elif dataset in riksdagen_se_datasets:
        _transform_riksdagen_data(dataset)


def demo_data_upload(dataset):
    """Uploads the transformed demo data depot to the demo server."""
    _can_handle_dataset(dataset)
    _needs_targetenv()
    rsync_project(env.demo_data_root, "%(demodata_dir)s/%(dataset)s-depot" % venv(), exclude=".*", delete=True)


def demo_build_war(dataset):
    """Builds a webapp capable of serving an uploaded demo data depot."""
    local("cd %(java_packages)s/demodata-supply && "
            "mvn -Ddataset=%(dataset)s -Ddemodata-root=%(demo_data_root)s clean package" % venv(), capture=False)

def demo_deploy_war(dataset):
    """Deploys an uploaded demo webapp"""
    _can_handle_dataset(dataset)
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    _deploy_war("%(java_packages)s/demodata-supply/target/%(dataset)s-demodata-supply.war" % venv(),
            "%(dataset)s-demodata-supply" % venv())


def demo_refresh(dataset):
    """
    Downloads, transforms, uploads, builds and deploys a webapp for serving a
    demo dataset.
    """
    _can_handle_dataset(dataset)
    demo_data_download(dataset)
    demo_data_to_depot(dataset)
    demo_data_upload(dataset)
    demo_build_war(dataset)
    demo_deploy_war(dataset)


def _mkdir_keep_prev(dir_path):
    if p.isdir("%s-prev"%dir_path):
        local("rm -rf %s-prev"%dir_path)
    if p.isdir("%s"%dir_path):
        local("mv %s %s-prev"%(dir_path, dir_path))
    local("mkdir -p %s"%dir_path)


def _download_lagen_nu_data(dataset):
    local("curl https://lagen.nu/%(dataset)s/parsed/rdf.nt -o lagennu-%(dataset)s.nt" % vars())

def _transform_lagen_nu_data(dataset):
    local("%(java_opts)s groovy %(demodata_tools)s/lagen_nu/n3dump_to_depot.groovy "
            " %(demodata_dir)s/%(dataset)s-raw/lagennu-%(dataset)s.nt %(demodata_dir)s/%(dataset)s" % venv())


def _download_riksdagen_data(dataset):
    local("%(java_opts)s groovy %(demodata_tools)s/data_riksdagen_se/fetch_data_riksdagen_se.groovy "
            " %(demodata_dir)s/%(dataset)s-raw %(dataset)s -f" % venv())

def _transform_riksdagen_data(dataset):
    local("groovy %(demodata_tools)s/data_riksdagen_se/depot_from_data_riksdagen_se.groovy "
            " %(demodata_dir)s/%(dataset)s-raw %(demodata_dir)s/%(dataset)s-depot" % venv())


