from fabric.api import *

@runs_once
def local_lib_rinfo_pkg():
    local("cd %(java_packages)s/ && mvn install"%x(), capture=False)
    # TODO:? This also "installs" final war:s etc.. Use mvn-param for install dest.?

def _deploy_war(localwar, warname):
    _needs_deployenv()
    put(localwar, "%(dist_dir)s/%(warname)s.war"%x())
    sudo("%(tomcat_stop)s"%x())
    sudo("rm -rf %(tomcat_webapps)s/%(warname)s/"%x())
    sudo("mv %(dist_dir)s/%(warname)s.war %(tomcat_webapps)s/"%x())
    sudo("%(tomcat_start)s"%x())

