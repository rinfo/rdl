import sys
x = lambda: dict(env, **sys._getframe(1).f_locals)

def slashed(path):
    return path if path.endswith('/') else path+'/'

# Hack to tell fabric these aren't tasks
from fabric.main import _internals
_internals += [x, slashed]
