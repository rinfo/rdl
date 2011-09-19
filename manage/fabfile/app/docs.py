from __future__ import with_statement
from fabric.api import env, local, roles, task
from fabric.contrib.project import rsync_project
from fabfile.util import slashed, cygpath
from fabfile.target import _needs_targetenv
import sys

@task
def build():
    local("cd %(toolsdir)s &&"
            " groovy build_rinfo_docs.groovy --clean %(docbuild)s"%env)

@task
@roles('doc')
def deploy():
    _needs_targetenv()
    if sys.platform == 'win32':
        build_path = cygpath(slashed(env.docbuild))
    else:
        build_path = slashed(env.docbuild)
    rsync_project(env.docs_webroot, build_path,
            exclude=".*", delete=True)

