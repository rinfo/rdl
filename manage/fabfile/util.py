from fabric.api import *
from fabric.contrib.files import exists
import sys
import time

from os import path as p

venv = lambda: dict(env, **sys._getframe(1).f_locals)

def fullpath(dirpath):
    return p.abspath(p.normpath(dirpath))

def mkdirpath(path):
    if not exists(path): run("mkdir -p %s" % path)

def slashed(path):
    return path if path.endswith('/') else path+'/'

def cygpath(path):
    return local("cygpath %s" % path)

def msg_sleep(sleepTime, msg=""):
    print "Pause in {0} second(s) for {1}!".format(sleepTime,msg)
    time.sleep(sleepTime)

def verify_url_content(url, string_exists_in_content):
    respHttp = local("curl %(url)s"%vars(), capture=True)
    if not string_exists_in_content in respHttp:
        print "Could not find %(string_exists_in_content)s in response! Failed!"%vars()
        print "#########################################################################################"
        print respHttp
        print "#########################################################################################"
        return False
    return True
