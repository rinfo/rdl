from __future__ import with_statement
from fabric.api import *
from fabric.contrib.project import rsync_project


env.projectroot = '../..'
env.toolsdir = "%(projectroot)s/tools" % env
env.builddir = '%(toolsdir)s/_build/documentation' % env
env.hosts = ["dev.lagrummet.se"]
env.webroot = "/var/www/dokumentation/v0" # TODO: change to official when OK:d


def build():
    local("cd %(toolsdir)s; groovy build_rinfo_docs.groovy --clean" % env)

def upload():
    rsync_project(env.webroot, _slashed(env.builddir), exclude=".*", delete=True)


def _slashed(path):
    return path if path.endswith('/') else path+'/'

