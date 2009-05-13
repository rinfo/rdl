from fabric.api import *
from datetime import datetime
from fmt import fmt

from deploy.envs import *
from deploy.rinfo_main import *
from deploy.rinfo_service import *
#from deploy.rinfo_testsources import *

env.project = 'rinfo'
env.projectroot = '..'
env.base_data = fmt("$projectroot/resources/base")
env.java_packages = fmt("$projectroot/packages/java")
env.timestamp = datetime.utcnow().strftime('%Y_%m_%d_%H-%M-%S')

