########################################################################
README - Acceptance Tests
########################################################################

The acceptance tests are run with Robot Framework
(<http://robotframework.org/>). To get started, use::

    $ sudo easy_install robotframework
    $ sudo easy_install http://robotframework-restlibrary.googlecode.com/svn/trunk#egg=RestLibrary-dev
    $ sudo easy_install lxml

, and, optionally:

    $ easy_install httplib2

Then run:

    $ pybot .

to run the full acceptance test suite.

