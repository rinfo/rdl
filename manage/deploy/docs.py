from __future__ import with_statement
from fabric.api import *
from fabric.contrib.project import rsync_project


def prod_doc_target():
    env.toolsdir = "%(projectroot)s/tools" % env
    env.builddir = '%(toolsdir)s/_build/documentation' % env
    env.hosts = ["dev.lagrummet.se"]
    env.webroot = "/var/www/dokumentation/v0" # TODO: change to official when OK:d


def build_docs():
    prod_doc_target()
    local("cd %(toolsdir)s; groovy build_rinfo_docs.groovy --clean" % env)

def upload_docs():
    prod_doc_target()
    rsync_project(env.webroot, _slashed(env.builddir), exclude=".*", delete=True)


def _slashed(path):
    return path if path.endswith('/') else path+'/'

