from __future__ import with_statement
from fabric.api import env, local, roles
from fabric.contrib.project import rsync_project
from util import slashed
from targetenvs import _needs_targetenv


def build_docs():
    local("cd %(toolsdir)s &&"
            " groovy build_rinfo_docs.groovy --clean %(docbuild)s"%env)

@roles('doc')
def deploy_docs():
    _needs_targetenv()
    rsync_project(env.docs_webroot, slashed(env.docbuild), exclude=".*", delete=True)

