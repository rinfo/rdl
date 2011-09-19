from os import sep
import sys
from fabric.api import *
from fabric.contrib.files import exists
from fabric.contrib.project import rsync_project
from fabfile.util import slashed, cygpath
from fabfile.target import _needs_targetenv
from fabfile.util import venv, fullpath


env.adminbuild = sep.join((env.builddir, 'rinfo-admin'))


@task
@runs_once
@roles('admin')
def setup():
    _needs_targetenv()
    if not exists(env.admin_webroot):
       sudo("mkdir %(admin_webroot)s" % env)
       sudo("chown %(user)s %(admin_webroot)s" % env)

@task
def package(sources=None, outdir=None):
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

@task
@roles('admin')
def deploy(builddir=None):
    """Deploy the admin feed to target env."""
    setup()
    builddir = builddir or env.adminbuild
    if sys.platform == 'win32':
        # There is no native rsync for windows, only the cygwin
        # version. We must convert the windows-style path of
        # builddir to a cygwin-style equivalent
        build_path = cygpath(slashed(builddir))
    else:
        build_path = slashed(builddir)
    rsync_project((env.admin_webroot), build_path, exclude=".*", delete=True)

@task
@roles('admin')
def all():
    """Package and deploy the admin feed."""
    package()
    deploy()

@task
def ping_main():
    """Ping rinfo-main to (re-)collect the admin feed"""
    _needs_targetenv()
    feed_url = "http://%s/feed/current" % env.roledefs['admin'][0]
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    print local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())

