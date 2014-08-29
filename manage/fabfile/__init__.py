"""
RInfo Management Fabric
"""
from datetime import datetime
from os import path as p
from os import sep
from fabric.api import env


##
# Global settings

env.project = 'rinfo'

env.manageroot = p.normpath(p.join(p.dirname(__file__), '..'))

# NOTE: must be an absolute path:
env.projectroot = p.normpath(p.join(env.manageroot, '..'))

env.toolsdir = sep.join((env.projectroot, 'tools'))

env.builddir = sep.join((env.projectroot, '_build'))

env.docbuild = sep.join((env.builddir, 'documentation'))

env.baseresources = "%(projectroot)s/resources/base"%env

# env.java_packages = "%(projectroot)s/packages/java"%env
env.java_packages = sep.join((env.projectroot,'packages', 'java'))

env.java_pkg_version = "1.0-SNAPSHOT"
env.timestamp = datetime.utcnow().strftime('%Y_%m_%d_%H-%M-%S')
env.datestamp = datetime.utcnow().strftime('%Y-%m-%d')

env.ftp_server_url = "ftp://archive0d.glesys.com"
env.snapshot_name = ""

# env.roledefs defines available roles but the actual host lists for a certain
# role is environment dependent and set up by the targets defined in
# target.py (see i.e. tg_dev_unix)
env.roledefs = {
    'doc': [],
    'main': [],
    'admin': [],
    'service': [],
    'checker': [],
    'demosource': [],
    'lagrummet': [],
}

##
# Import tasks

import target
import sysconf
import server
import app.main
import app.admin
import app.service
import app.checker
import app.docs
import app.tools
import app.demodata

