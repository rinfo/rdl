from usefab import *
from datetime import datetime

from deploy.envs import *
from deploy.rinfo_main import *
from deploy.rinfo_service import *
#from deploy.rinfo_testsources import *

env.project = 'rinfo'
env.projectroot = '..'
env.base_data = v("$projectroot/resources/base")
env.java_packages = v("$projectroot/packages/java")
env.timestamp = datetime.utcnow().strftime('%Y_%m_%d_%H-%M-%S')

