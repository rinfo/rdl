from fabric.api import env, local, roles
from fabric.contrib.project import rsync_project
from util import slashed, cygpath
from targetenvs import _needs_targetenv
from os import sep
import sys


env.adminbuild = sep.join((env.builddir, 'rinfo-admin'))


def package_admin(dataset_sources=None):
    """Package the admin feed files into a servable directory"""
    sourceflag = "-s %s"%(dataset_sources) if dataset_sources else ""

    # TODO: if dataset_sources: make sure adminbuild dir is not the default (to
    # avoid deploying test sources to prod..)?
    local(("cd %(toolsdir)s/rinfomain && " +
            " groovy base_as_feed.groovy " +
            " -b %(baseresources)s " +
            sourceflag +
            " -o %(adminbuild)s") % env)

@roles('admin')
def deploy_admin():
    """Deploy the admin feed"""
    _needs_targetenv()
    if sys.platform == 'win32':
        # There is no native rsync for windows, only the cygwin
        # version. We must convert the windows-style path of
        # env.adminbuild to a cygwin-style equivalent
        build_path = cygpath(slashed(env.adminbuild))
    else:
        build_path = slashed(env.adminbuild)
    rsync_project((env.admin_webroot), build_path, exclude=".*", delete=True)

@roles('admin')
def admin_all():
    """Package and deploy the admin feed"""
    package_admin()
    deploy_admin()

def ping_main_with_admin():
    """Ping rinfo-main to (re-)collect the admin feed"""
    _needs_targetenv()
    feed_url = "http://%s/feed/current" % env.roledefs['admin'][0]
    collector_url = "http://%s/collector/" % env.roledefs['main'][0]
    print local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())

