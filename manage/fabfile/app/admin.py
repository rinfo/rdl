import sys
from os import sep, path as p
from fabric.api import *
from fabric.contrib.files import exists
from fabric.contrib.project import rsync_project
from fabfile.util import slashed, cygpath
from fabfile.target import _needs_targetenv
from fabfile.util import venv, fullpath


def get_build_dir():
    return sep.join((env.builddir, env.target, 'rinfo-admin'))


@task
@runs_once
@roles('admin')
def setup():
    _needs_targetenv()
    if not exists(env.admin_webroot):
       sudo("mkdir %(admin_webroot)s" % env)
       sudo("chown %(user)s %(admin_webroot)s" % env)

@task
def package():
    """
    Package the admin feed files into a servable directory.
    """
    tg_sources = p.join(env.projectroot, "resources", env.target, "datasources.n3")
    sourceopt = "-s " + fullpath(tg_sources) if p.exists(tg_sources) else ""
    outdiropt ="-o " + fullpath(get_build_dir())
    local("cd %(toolsdir)s/rinfomain && groovy base_as_feed.groovy "
            " -b %(baseresources)s %(sourceopt)s %(outdiropt)s" % venv())

@task
@roles('admin')
def deploy():
    """Deploy the admin feed to target env."""
    setup()
    builddir = get_build_dir()
    if sys.platform == 'win32':
        # Support cygwin rsync on windows:
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

