"""
RInfo Management Fabric
"""
import datetime
from os import path as p
from fabric.api import env


##
# Global settings

env.project = 'rinfo'

env.projectroot = p.normpath(p.join(p.dirname(__file__), '..'))

env.toolsdir = "%(projectroot)s/tools"%env
env.builddir = "%(projectroot)s/_build"%env
env.docbuild = '%(builddir)s/documentation'%env

env.base_data = "%(projectroot)s/resources/base"%env
env.java_packages = "%(projectroot)s/packages/java"%env
env.java_pkg_version = "1.0-SNAPSHOT"
env.timestamp = datetime.datetime.utcnow().strftime('%Y_%m_%d_%H-%M-%S')
env.datestamp = datetime.datetime.utcnow().strftime('%Y-%m-%d')

env.roledefs = {
    'doc': None,
    'main': None,
    'admin': None,
    'service': None,
    'checker': None,
}

##
# Import tasks

from targetenvs import *
from sysconf import *
from servertools import *
from deploy.rinfo_main import *
from deploy.rinfo_admin import *
from deploy.rinfo_service import *
from deploy.rinfo_checker import *
from deploy.docs import *
from apptools import *

