from fabric.api import env
import datetime

from deploy.envs import *
from deploy.rinfo_main import *
from deploy.rinfo_service import *
#from deploy.rinfo_testsources import *

env.project = 'rinfo'
env.projectroot = '..'
env.base_data = "%(projectroot)s/resources/base"%env
env.java_packages = "%(projectroot)s/packages/java"%env
env.timestamp = datetime.datetime.utcnow().strftime('%Y_%m_%d_%H-%M-%S')

