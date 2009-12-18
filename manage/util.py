
from fabric.api import env
import sys
venv = lambda: dict(env, **sys._getframe(1).f_locals)

def slashed(path):
    return path if path.endswith('/') else path+'/'


def dirpath(path):
    if not exists(path): run("mkdir -p %s" % path)

# Hack to tell fabric these aren't tasks
from fabric.main import _internals
_internals += [venv, slashed, dirpath]

