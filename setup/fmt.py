import sys
from string import Template
from fabric.api import env

class ExtendedTemplate(Template):
    idpattern = r'[_a-z0-9]+'
    def vararg_safe_substitute(self, args, kwargs):
        # TODO: choose between positional or one dict non-kw arg
        if len(args) == 1 and isinstance(args[0], dict):
            kwargs.update(args[0])
        else:
            kwargs.update((str(i), v) for i, v in enumerate(args))
        return self.safe_substitute(kwargs)

def fmt(s, *args, **kwargs):
    """
    Recursively interpolate values into the given format string, using
    ``string.Template``.

    The values are drawn from the keyword arguments, variable names in the
    *calling* scope and ``env``, in that order.
    #-- TODO: or if _getframe hack is removed (needed for recursion) --
    The values are drawn from the keyword arguments and ``env``, in that order.

    The recursion means that if a value to be interpolated is itself a format
    string, then it will be processed as well.

    If a name cannot be found in the keyword arguments or ``env``, then that
    format-part will be left untouched.

    Examples::

        >>> fmt("$shell")
        '/bin/bash -l -c'

        >>> fmt("$shell", shell="ksh")
        'ksh'

    Missing references are left unexpanded::

        >>> fmt("$shell 'echo $PWD'")
        "/bin/bash -l -c 'echo $PWD'"

    References can be escaped::

        >>> fmt("$$shell")
        '$shell'

    #Nested string references are recursively expanded::
    #
    #   >>> fmt("a is ($a)", a="b is ($b/$c) and $d", b=1, c=2)
    #   'a is (b is (1/2) and $d)'

    #-- TODO: or take one positional and treat as dict (here using vars()) --
    Positional arguments can be referenced by number::

        >>> fmt("$cmd $0 $0.$1", "file", "bak", cmd="mv")
        'mv file file.bak'

    Local variables are also captured::

        >>> shell = "tcsh"
        >>> fmt("$shell")
        'tcsh'
        >>> del shell

    If first positional argument is a dict, use it as keywords::

        >>> fmt("$shell", {'shell': "ipython"})
        'ipython'

    """
    data = {}
    data.update(env)
    data.update(sys._getframe(1).f_locals)
    data.update(kwargs)
    #for k, v in data.items():
    #   if isinstance(v, basestring) and '$' in v:
    #       del data[k] # recursion base-case is empty map
    #       data[k] = fmt(v, **data)
    return ExtendedTemplate(s).vararg_safe_substitute(args, data)

