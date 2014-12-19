from fabric.api import *
from fabric.contrib.files import exists
from fabric.contrib.project import rsync_project
from fabfile.util import slashed, cygpath, exit_on_error
from fabfile.target import _needs_targetenv
import sys


@task
def build():
    local("cd %(toolsdir)s &&"
          " groovy build_rinfo_docs.groovy --clean %(docbuild)s" % env)


@task
@roles('doc')
def setup():
    _needs_targetenv()
    if not exists(env.docs_webroot):
        sudo("mkdir %(docs_webroot)s" % env)
        sudo("chown %(user)s %(docs_webroot)s" % env)


@task
@roles('doc')
@exit_on_error
def deploy():
    setup()
    build_path = slashed(env.docbuild)
    if sys.platform == 'win32':
        build_path = cygpath(build_path)
    rsync_project(env.docs_webroot, build_path, exclude=".*", delete=True)
    sudo("chmod -R 755 %(docs_webroot)s" % env)