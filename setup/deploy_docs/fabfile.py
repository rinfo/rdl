from __future__ import with_statement
from fabric.api import *


env.projectroot = '../..'
env.toolsdir = "%(projectroot)s/tools" % env
env.builddir = '%(toolsdir)s/_build' % env
env.webroot = "/opt/rinfo/www"


def build():
    local("cd %(toolsdir)s; groovy build_rinfo_docs.groovy" % env)


def upload():
    raise NotImplementedError("Needs server-info, deploy-path etc.")
    rsync_project(webroot, env.builddir, exclude=".*", delete=True)

