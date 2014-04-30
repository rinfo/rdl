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

@task
def msg_sleep(sleep_time, msg=""):
    print "Pause in {0} second(s) for {1}!".format(sleep_time,msg)
    time.sleep(sleep_time)

def verify_url_content(url, string_exists_in_content, sleep_time=15, max_retry=3):
    retry_count = 1
    while retry_count < max_retry:
        respHttp = local("curl %(url)s"%vars(), capture=True)
        if not string_exists_in_content in respHttp:
            print "Could not find '%(string_exists_in_content)s' in response! Failed! Retry %(retry_count)s of %(max_retry)s."%vars()
            retry_count = retry_count + 1
            time.sleep(sleep_time)
            continue
        return True
    print "#########################################################################################"
    print respHttp
    print "#########################################################################################"
    return False