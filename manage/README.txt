########################################################################
README - Setup
########################################################################

Prerequisites
========================================================================

The RInfo project is managed using Fabric:

    http://www.fabfile.org/

On systems where python-setuptools is bundled (hence having the
``easy_install`` command), it can be installed by::

    $ sudo easy_install Fabric

This is the case on OS X 10.5+. On Ubuntu, you may first need to::

    $ sudo apt-get install python-dev python-setuptools

For other systems, see the Fabric website (above) for instructions.

Managing
========================================================================

To find out which commands are available, run:

    $ fab -l


To build, deploy and make rinfo-main load the admin data:

    $ fab package_admin

This creates a directory hierarchy containing RDF and XHTML data,
based on the RDF files located in resources/base. This contains the
model (ontology), uri patterns, URIs for organizations and other
resources.

    $ fab tg_prod package_admin

This rsync's the resulting directory hierarchy to the correct place in
the production environment (substitute tg_dev_unix for tg_prod if you
want to deploy them to your own local development environment)


    $ fab tg_prod ping_main_with_admin

This sends a "ping" request to the rinfo-main server, causing it to
reload the admin data.

Development Environment
========================================================================

For continuous integration see ``project/ci/README.txt``.

