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

env.roledefs = {'main': None, 'service': None, 'doc': None}

##
# Import tasks

from targetenvs import *
from servertools import *
from deploy.rinfo_main import *
from deploy.rinfo_service import *
#from deploy.rinfo_testsources import *
from deploy.docs import *
from sysconf import *

##
# Runtime operations

def ping_main_collector():
    #require('roledefs', provided_by=targetenvs)
    collector_url = "http://%s/collector/" % env.roledefs['main'][0]
    feed_url = "http://%s:8182/feed/current" % env.roledefs['examples'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())

def ping_service_collector():
    #require('roledefs', provided_by=targetenvs)
    collector_url = "http://%s/collector/" % env.roledefs['service'][0]
    feed_url = "http://%s/feed/current" % env.roledefs['main'][0]
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())

