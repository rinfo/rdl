from fabric.api import *
from fabric.contrib.files import exists
import sys
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

