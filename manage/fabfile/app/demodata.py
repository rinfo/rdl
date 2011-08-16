from __future__ import with_statement
from fabric.api import env, local, cd, roles
from fabric.contrib.files import *
from fabric.contrib.project import rsync_project
from fabfile.target import _needs_targetenv
from fabfile.app import _deploy_war
from fabfile.app import admin
from fabfile.util import venv
from os import path as p


env.java_opts = 'JAVA_OPTS="-Xms512m -Xmx1024m"'
env.demodata_dir = "/opt/_workapps/rinfo/demodata"
env.demodata_tools = p.join(env.projectroot, "tools", "demodata")


lagen_nu_datasets = ('sfs', 'dv')
riksdagen_se_datasets = ('prop', 'sou', 'ds')

def _can_handle_dataset(dataset):
    if not any(dataset in ds for ds in (lagen_nu_datasets, riksdagen_se_datasets)):
        raise ValueError("Undefined dataset %r" % dataset)


@task
def download(dataset, force="1"):
    """Downloads a demo dataset from its source."""
    _can_handle_dataset(dataset)
    if not int(force) and p.isdir("%(demodata_dir)s/%(dataset)s-raw" % venv()):
        return
    _mkdir_keep_prev("%(demodata_dir)s/%(dataset)s-raw" % venv())
    with cd("%(demodata_dir)s/%(dataset)s-raw" % venv()):
        if dataset in lagen_nu_datasets:
            _download_lagen_nu_data(dataset)
        elif dataset in riksdagen_se_datasets:
            _download_riksdagen_data(dataset)

@task
def create_depot(dataset):
    """Transforms the downloaded demo data to a depot."""
    _can_handle_dataset(dataset)
    _mkdir_keep_prev("%(demodata_dir)s/%(dataset)s" % venv())
    if dataset in lagen_nu_datasets:
        _transform_lagen_nu_data(dataset)
    elif dataset in riksdagen_se_datasets:
        _transform_riksdagen_data(dataset)

@task
@roles('demo')
def upload(dataset):
    """Upload the transformed demo data depot to the demo server."""
    _can_handle_dataset(dataset)
    _needs_targetenv()
    if not exists(env.demo_data_root):
       sudo("mkdir -p %(demo_data_root)s" % env)
       sudo("chown %(user)s %(demo_data_root)s" % env)
    rsync_project(env.demo_data_root, "%(demodata_dir)s/%(dataset)s" % venv(), exclude=".*", delete=True)


#@task
def build_dataset_war(dataset):
    """Build a webapp capable of serving an uploaded demo data depot."""
    local("cd %(java_packages)s/demodata-supply && "
            "mvn -Ddataset=%(dataset)s -Ddemodata-root=%(demo_data_root)s clean package" % venv(), capture=False)

#@task
@roles('demo')
def deploy_dataset_war(dataset):
    """Deploy a demo webapp for the given uploaded dataset."""
    _can_handle_dataset(dataset)
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    _deploy_war("%(java_packages)s/demodata-supply/target/%(dataset)s-demodata-supply.war" % venv(),
            "%(dataset)s-demodata-supply" % venv())

@task
@roles('demo')
def dataset_war(dataset):
    """Package and deploy a demo webapp for the given uploaded dataset."""
    build_dataset_war(dataset)
    deploy_dataset_war(dataset)

@task
def refresh(dataset, force="0"):
    """
    Download, transform, upload, build and deploy a webapp for serving a demo
    dataset.
    """
    _can_handle_dataset(dataset)
    download(dataset, force)
    create_depot(dataset)
    upload(dataset)
    dataset_war(dataset)


@task
@roles('admin')
def demo_admin():
    """
    Create and deploy a static admin webapp configured for the demo datasets.
    """
    admin.setup()
    adminbuild = p.join(env.demodata_dir, "rinfo-admin-demo")
    sources = p.join(env.projectroot, "resources", env.target, "datasources.n3")
    admin.package(sources, adminbuild)
    admin.deploy(adminbuild)


#def full_demo_deploy():
#    from itertools import chain
#    for dataset in chain(lagen_nu_datasets, riksdagen_se_datasets):
#        # TODO:? env.role = dataset
#        demo_refresh(dataset)
#    demo_admin()


def _mkdir_keep_prev(dir_path):
    if p.isdir("%s-prev"%dir_path):
        local("rm -rf %s-prev"%dir_path)
    if p.isdir("%s"%dir_path):
        local("mv %s %s-prev"%(dir_path, dir_path))
    local("mkdir -p %s"%dir_path)


def _download_lagen_nu_data(dataset):
    local("curl https://lagen.nu/%(dataset)s/parsed/rdf.nt -o %(demodata_dir)s/%(dataset)s-raw/lagennu-%(dataset)s.nt" % venv())

def _transform_lagen_nu_data(dataset):
    local("%(java_opts)s groovy %(demodata_tools)s/lagen_nu/n3dump_to_depot.groovy "
            " %(demodata_dir)s/%(dataset)s-raw/lagennu-%(dataset)s.nt %(demodata_dir)s/%(dataset)s" % venv())


def _download_riksdagen_data(dataset):
    local("%(java_opts)s groovy %(demodata_tools)s/data_riksdagen_se/fetch_data_riksdagen_se.groovy "
            " %(demodata_dir)s/%(dataset)s-raw %(dataset)s -f" % venv())

def _transform_riksdagen_data(dataset):
    local("%(java_opts)s groovy %(demodata_tools)s/data_riksdagen_se/depot_from_data_riksdagen_se.groovy "
            " %(demodata_dir)s/%(dataset)s-raw %(demodata_dir)s/%(dataset)s" % venv())


__all__ = tuple(key for key in globals() if key != 'admin')
