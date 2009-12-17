from __future__ import with_statement
from fabric.api import *
from fabric.contrib.project import rsync_project
from util import slashed


def doc_server():
    env.hosts = ["dev.lagrummet.se"]
    env.webdocroot = "/var/www/dokumentation/v0" # TODO: change to official when OK:d

def build_docs():
    local("cd %(toolsdir)s &&"
            " groovy build_rinfo_docs.groovy --clean %(docbuild)s" % env)

def upload_docs():
    doc_server()
    rsync_project(env.webdocroot, slashed(env.docbuild), exclude=".*", delete=True)

