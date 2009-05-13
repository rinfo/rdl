import sys
from string import Template
from fabric.api import env

def fmt(s, *args, **kwargs):
    """
    This variable expansion utility attempts (using ``string.Template``) to
    substitute variable references in the supplied string. The order of lookup is:

    1. Keyword arguments (if any).
    2. Variables in the *calling* scope.
    3. A variable defined in fabric's ``env`` namespace.

    Examples::

        >>> fmt("$shell $notexpanded")
        '/bin/bash -l -c $notexpanded'

        >>> shell = "local"
        >>> fmt("$shell $notexpanded")
        'local $notexpanded'

        >>> fmt("$shell $notexpanded", shell="other")
        'other $notexpanded'

        >>> fmt("$$shell $notexpanded", shell="other")
        '$shell $notexpanded'

    """
    data = {}
    data.update(env)
    data.update(sys._getframe(1).f_locals)
    data.update(kwargs)
    return Template(s).safe_substitute(data)

