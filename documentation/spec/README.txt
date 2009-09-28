########################################################################
README - Acceptance Tests
########################################################################

The acceptance tests are run with Robot Framework
(<http://robotframework.org/>). To get started, use::

    $ sudo easy_install robotframework # or use installer for windows
    $ sudo easy_install http://robotframework-restlibrary.googlecode.com/svn/trunk#egg=RestLibrary-dev
    $ sudo easy_install lxml
    $ easy_install httplib2 # optional

Then run::

    $ pybot .

to execute all test suites. Or e.g.::

    $ pybot rinfo/

For a specific suite.

You can also override a variable, e.g.::

    $ pybot --variable BASE_URL:http://localhost:7000 supplier/


Tips
========================================================================

To serve a directory with a local web server, go to that dir and run e.g.::

    $ python -m SimpleHTTPServer

Or for a different port::

    $ python -m SimpleHTTPServer 7000


Test libraries
========================================================================

Local test keywords are defined in the ``robotlib`` directory.

Some of them contain doctests to verify their behaviour. Run these with::

        $ nosetests --with-doctest robotlib/


