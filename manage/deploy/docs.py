from __future__ import with_statement
from fabric.api import env, local, roles
from fabric.contrib.project import rsync_project
from util import slashed, cygpath
from targetenvs import _needs_targetenv
import sys

def build_docs():
    local("cd %(toolsdir)s &&"
            " groovy build_rinfo_docs.groovy --clean %(docbuild)s"%env)

@roles('doc')
def deploy_docs():
    _needs_targetenv()
    if sys.platform == 'win32':
        build_path = cygpath(slashed(env.docbuild))
    else:
        build_path = slashed(env.docbuild)
    rsync_project(env.docs_webroot, build_path,
            exclude=".*", delete=True)

