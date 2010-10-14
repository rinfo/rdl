from os import sep
import sys
from fabric.api import env, local, roles
from fabric.contrib.project import rsync_project
from util import slashed, cygpath
from targetenvs import _needs_targetenv
from util import venv, fullpath


env.adminbuild = sep.join((env.builddir, 'rinfo-admin'))


def package_admin(sources=None, outdir=None):
    """
    Package the admin feed files into a servable directory.
    """
    # make sure adminbuild dir is not the default (to avoid deploying
    # experimental sources to prod)
    if sources:
        assert outdir and fullpath(outdir) != fullpath(env.adminbuild)
        sources = fullpath(sources)
        outdir = fullpath(outdir)
    sourceopt = ("-s %s" % sources) if sources else ""
    outdiropt ="-o %s" % (outdir or env.adminbuild)
    local("cd %(toolsdir)s/rinfomain && groovy base_as_feed.groovy "
            " -b %(baseresources)s %(sourceopt)s %(outdiropt)s" % venv())

@roles('admin')
def deploy_admin(builddir=None):
    """Deploy the admin feed"""
    _needs_targetenv()
    builddir = builddir or env.adminbuild
    if sys.platform == 'win32':
        # There is no native rsync for windows, only the cygwin
        # version. We must convert the windows-style path of
        # builddir to a cygwin-style equivalent
        build_path = cygpath(slashed(builddir))
    else:
        build_path = slashed(builddir)
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

