from __future__ import with_statement
from fabric.api import env, local, cd
from fabric.contrib.files import *


env.java_opts = 'JAVA_OPTS="-Xms512Mb -Xmx1024Mb"'
env.test_data_dir = "/opt/_workapps/rinfo/testdata"
env.test_data_tools = "../tools/testscenarios"


def download_demodata():
    _mkdir_keep_prev("%(test_data_dir)s/lagen-nu"%env)

    with cd("%(test_data_dir)s/lagen-nu"%env):
        local("curl https://lagen.nu/sfs/parsed/rdf.nt -o lagennu-sfs.nt")
        #local("curl https://lagen.nu/dv/parsed/rdf.nt -o lagennu-dv.nt")

def demodata_to_depot():
    _mkdir_keep_prev("%(test_data_dir)s/sfs-depot"%env)

    local("%(java_opts)s groovy %(test_data_tools)s/n3dump_to_depot.groovy %(test_data_dir)s/lagen-nu/lagennu-sfs.nt %(test_data_dir)s/sfs-depot"%env)

#def serve_depot(depot=None, port=8180):
#    depot = depot or "%s/lagen-nu/%s" % (env.test_data_dir, "sfs-depot")
#    local("groovy %s/serve_depot.groovy %s 8180" % (env.test_data_tools, depot))
#     # Ping service:
#    local('curl --data "feed=http://localhost:8180/feed/current" http://localhost:8181/collector')

def _mkdir_keep_prev(dir_path):
    if exists("%s-prev"%dir_path):
        local("rm -rf %s-prev"%dir_path)

    if exists("%s"%dir_path):
        local("mv %s %s-prev"%(dir_path, dir_path))

    local("mkdir -p %s"%dir_path)

