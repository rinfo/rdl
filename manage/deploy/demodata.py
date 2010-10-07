from __future__ import with_statement
from fabric.api import env, local, cd
from fabric.contrib.files import *
from fabric.contrib.project import rsync_project
from targetenvs import _needs_targetenv
from deploy import _deploy_war


env.java_opts = 'JAVA_OPTS="-Xms512Mb -Xmx1024Mb"'
env.test_data_dir = "/opt/_workapps/rinfo/testdata"
env.test_data_tools = "../tools/testscenarios"


def demo_data_download_sfs():
    _mkdir_keep_prev("%(test_data_dir)s/lagen-nu-sfs"%env)

    with cd("%(test_data_dir)s/lagen-nu-sfs"%env):
        local("curl https://lagen.nu/sfs/parsed/rdf.nt -o lagennu-sfs.nt")

def demo_data_to_depot_sfs():
    _mkdir_keep_prev("%(test_data_dir)s/sfs"%env)

    local("%(java_opts)s groovy %(test_data_tools)s/n3dump_to_depot.groovy %(test_data_dir)s/lagen-nu-sfs/lagennu-sfs.nt %(test_data_dir)s/sfs"%env)

def demo_data_upload_sfs():
    _needs_targetenv()
    rsync_project((env.demo_data_root), "%(test_data_dir)s/sfs"%env, exclude=".*", delete=True)

def demo_build_war_sfs():
    local("cd %(java_packages)s/demodata-supply && "
            "mvn -Ddataset=sfs -Ddemodata-root=%(demo_data_root)s clean package"%env, capture=False)

def demo_deploy_war_sfs():
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    _deploy_war("%(java_packages)s/demodata-supply/target/sfs-demodata-supply.war"%env,
            "sfs-demodata-supply")

def demo_refresh_sfs():
    """Downloads, transforms, uploads and starts to serve the demo data"""
    demo_data_download_sfs()
    demo_data_to_depot_sfs()
    demo_data_upload_sfs()
    demo_build_war_sfs()
    demo_deploy_war_sfs()


def demo_data_download_dv():
    _mkdir_keep_prev("%(test_data_dir)s/lagen-nu-dv"%env)

    with cd("%(test_data_dir)s/lagen-nu-dv"%env):
        local("curl https://lagen.nu/dv/parsed/rdf.nt -o lagennu-dv.nt")

def demo_data_to_depot_dv():
    _mkdir_keep_prev("%(test_data_dir)s/dv"%env)

    local("%(java_opts)s groovy %(test_data_tools)s/n3dump_to_depot.groovy %(test_data_dir)s/lagen-nu-dv/lagennu-dv.nt %(test_data_dir)s/dv"%env)

def demo_data_upload_dv():
    _needs_targetenv()
    rsync_project((env.demo_data_root), "%(test_data_dir)s/dv"%env, exclude=".*", delete=True)

def demo_build_war_dv():
    local("cd %(java_packages)s/demodata-supply && "
            "mvn -Ddataset=dv -Ddemodata-root=%(demo_data_root)s clean package"%env, capture=False)

def demo_deploy_war_dv():
    if not exists(env.dist_dir):
        run("mkdir %(dist_dir)s"%env)
    _deploy_war("%(java_packages)s/demodata-supply/target/dv-demodata-supply.war"%env,
            "dv-demodata-supply")

def demo_refresh_dv():
    """Downloads, transforms, uploads and starts to serve the demo data"""
    demo_data_download_dv()
    demo_data_to_depot_dv()
    demo_data_upload_dv()
    demo_build_war_dv()
    demo_deploy_war_dv()

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
