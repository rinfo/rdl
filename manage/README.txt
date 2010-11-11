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

    $ fab tg_prod deploy_admin

This rsync's the resulting directory hierarchy to the correct place in
the production environment (substitute tg_dev_unix for tg_prod if you
want to deploy them to your own local development environment)

    $ fab tg_prod ping_main_with_admin

This sends a "ping" request to the rinfo-main server, causing it to
reload the admin data.

Setting Up the Integration Environment
========================================================================

The integration environment is supposed to be run as a virtual server
on your local computer. It can host all rinfo applications. In the
instructions below it is assumed that local commands are run while
standing in the directory rinfo-trunk/manage

   * Setup a virtual server using a debian based linux distribution
   * Ensure that sudo is available (run: apt-get install sudo if not)
   * Ensure that sshd is available (run: sudo apt-get install openssh-server if not)
   * Ensure that you can ssh to the server preferably password less
   * Ensure that there is a package repository setup that contains sun-java6-jdk

      * For Debian:
         http://www.debian.org/doc/manuals/debian-java-faq/ch7.html ::
         sudo vim /etc/apt/sources.list ::
            deb <$HTTP-FTP...> $DIST main contrib non-free
         sudo apt-get update

      * For Ubuntu (10.04 Lucid):
         sudo add-apt-repository "deb http://archive.canonical.com/ lucid partner"
         sudo apt-get update

      * For Ubuntu (10.10 Maverick):
         sudo add-apt-repository "deb http://archive.canonical.com/ maverick partner"
         sudo apt-get update

   * Edit the hosts file on your local computer so that it contains:
      <IP-OF-VIRTUAL-SERVER>  rinfo-main rinfo-service rinfo-admin rinfo-checker
      <IP-OF-VIRTUAL-SERVER>  sfs-demo dv-demo prop-demo sou-demo ds-demo

   * Run on your local computer::
      fab tg_integration -R main install_dependencies fetch_tomcat_dist install_tomcat

   * Run on your virtual server::
      ~/mgr_work/install/4_install-jdk.sh

   * Run on your local computer::
      fab tg_integration -R main configure_server
      fab tg_integration -R service configure_server
      fab tg_integration -R demo configure_server
      fab tg_integration -R checker configure_server

   * Run on your virtual server::
      sudo /etc/init.d/apache2 graceful

The virtual server should now be ready for the deployment of the
applications and the demo data.

Deploy demo data from lagen.nu:

   * Run on your local computer::
      fab tg_integration -H dv-demo demo_refresh:dv
      fab tg_integration -H sfs-demo demo_refresh:sfs

Deploy demo data from riksdagen.se (very time consuming):

   * Run on your local computer::
      fab tg_integration -H prop-demo demo_refresh:prop
      fab tg_integration -H sou-demo demo_refresh:sou
      fab tg_integration -H ds-demo demo_refresh:ds

Deploy admin feed:

   * Run on your local computer::
      fab tg_integration demo_admin

Deploy main:

   * Run on your local computer::
      fab tg_integration main_all

Verify main:

   * http://rinfo-main/feed/current
      should contain a feed with models etc.

   * http://rinfo-main/system/log/
      should contain entries for each source that has been collected

   You might need to ping main to collect data from the demo sources:

      * fab tg_integration ping_main_collector:http\://dv-demo/feed/current
      * fab tg_integration ping_main_collector:http\://sfs-demo/feed/current
      * fab tg_integration ping_main_collector:http\://prop-demo/feed/current
      * fab tg_integration ping_main_collector:http\://sou-demo/feed/current
      * fab tg_integration ping_main_collector:http\://ds-demo/feed/current

Deploy checker:

   * Run on your local computer::
      fab tg_integration checker_all

Verify checker:

   * http://rinfo-checker/
      should contain the checker form. Put <http://dv-demo/feed/current> in 
      the URL input field and click Check to check the feed. This should 
      result in 10 green line being shown, one for each successfully checked 
      feed entry.

Deploy service (not verified to work yet):

   * Run on your local computer::
      fab tg_integration deploy_sesame
      fab tg_integration service_all

Development Environment
========================================================================

For continuous integration see ``project/ci/README.txt``.

