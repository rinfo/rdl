from fabric.api import *
from deploy.envs import *


##
# Local build

def package_examples(deps="1"):
    if int(deps): install_rinfo_pkg()
    require('deployenv', provided_by=deployenvs)
    local("cd %(java_packages)s/teststore-examples/ && mvn -P%(env)s package")

##
# Server deploy

def setup_examples():
    if not exists(env.dist_dir): run("mkdir %(dist_dir)s"%env)
    if not exists(env.rinfo_dir): sudo("mkdir %(rinfo_dir)s"%env)
    run("mkdir %(dist_dir)s"%env, fail='ignore')
    sudo("mkdir -p %(example_stores)s"%env, fail='ignore')

def upload_exampledata():
    setup_examples()
    tarname = "example.org.tar.gz"
    dest_tar = "%(dist_dir)s/%s" % tarname
    x = dict(env, **vars())
    put("%(projectroot)s/laboratory/exampledata/%(tarname)s"%x, dest_tar)
    sudo("rm -rf %(example_stores)s/example.org"%x, fail='warn')
    sudo("tar -C %(example_stores)s -xzf %s"%x)

def index_exampledata():
    wdir = "%(tomcat_webapps)s/ROOT/WEB-INF"
    clspath = '-cp $(for jar in $(ls lib/*.jar); do echo -n "$jar:"; done)'
    cmdclass = "se.lagrummet.rinfo.store.depot.FileDepotCmdTool"
    proppath = "classes/rinfo-depot.properties"
    sudo("sh -c 'cd %(wdir)s; java %(clspath)s %(cmdclass)s %(proppath)s index'"
            % vars())

def list_exampledata():
    run("ls %(example_stores)s/example.org/*/")

def deploy_examples():
    setup_examples()
    deploy_war("%(java_packages)s/teststore-examples/target/example-store-1.0-SNAPSHOT.war"%env,
            "ROOT.war")

def examples_all(deps="1"):
    package_examples(deps)
    upload_exampledata()
    index_exampledata()
    deploy_examples()

