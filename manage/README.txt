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

Or (for a nice, nested view of the namespaces):

    $ fab -lFnested

To build the admin data, run:

    $ fab app.admin.package

This creates a directory hierarchy containing RDF and XHTML data,
based on the RDF files located in resources/base. This contains the
model (ontology), uri patterns, URIs for organizations and other
resources.

To deploy this to production, do:

    $ fab taget.prod app.admin.deploy

This rsync's the resulting directory hierarchy to the correct place in the
production environment (substitute taget.prod with target.dev_unix if you want to
deploy them to your own local development environment, or target.demo if you want
to deploy them to the demo/staging environment)

Ping rinfo-main to load this data immediately by running:

    $ fab target.prod app.admin.ping_main

This sends a "ping" request to the rinfo-main server, causing it to
reload the admin data.

Setting Up the Integration Environment from Scratch
========================================================================

The integration environment is supposed to be run as a virtual server
on your local computer. It can host all rinfo applications. In the
instructions below it is assumed that local commands are run while
standing in the directory rinfo-trunk/manage

   * Setup a virtual server using a debian based linux distribution
   * Ensure that sudo is available (run: apt-get install sudo if not)
   * Ensure that sshd is available (run: sudo apt-get install openssh-server if not)
   * Ensure that you can ssh to the server, preferably passwordless
   * Ensure that the following tools are available: wget, curl, rsync
     and add-apt-repository (available in the ubuntu package
     python-software-properties)
   * Ensure that there is a package repository setup that contains sun-java6-jdk

      * For Debian:
         http://www.debian.org/doc/manuals/debian-java-faq/ch7.html ::
         sudo vim /etc/apt/sources.list ::
            deb <$HTTP-FTP...> $DIST main contrib non-free
         sudo apt-get update

      * For Ubuntu (10.04 Lucid):
         sudo add-apt-repository "deb http://archive.canonical.com/ lucid partner"
         sudo apt-get update

      * For Ubuntu (10.10 Maverick Meerkat):
         sudo add-apt-repository "deb http://archive.canonical.com/ maverick partner"
         sudo apt-get update

   * Edit the hosts file on your local computer so that it contains:
      <IP-OF-VIRTUAL-SERVER>  rinfo-integration
      <IP-OF-VIRTUAL-SERVER>  rinfo-main rinfo-service rinfo-admin rinfo-checker
      <IP-OF-VIRTUAL-SERVER>  sfs-demo dv-demo prop-demo sou-demo ds-demo

   * Create the rinfo user on your virtual server:
      sudo groupadd rinfo
      sudo useradd rinfo -g rinfo -d /home/rinfo/ -s /bin/bash -m
      sudo passwd rinfo # Choose a password
      sudo visudo # Append to the bottom: rinfo ALL=ALL

   * Run on your local computer::
      fab target.integration -R main sysconf.install_server

   * Run on your virtual server::
      ~/mgr_work/install/4_install-jdk.sh

   * Run on your local computer::
      fab target.integration -R main,service,checker,demo sysconf.configure_server server.reload_apache

The virtual server should now be ready for the deployment of the applications
and the demo data.

Deploy demo data from lagen.nu:

   * Run on your local computer::
      fab target.integration app.demodata.refresh:dv
      fab target.integration app.demodata.refresh:sfs

Deploy demo data from riksdagen.se (very time consuming):

   * Run on your local computer::
      fab target.integration app.demodata.refresh:prop
      fab target.integration app.demodata.refresh:sou
      fab target.integration app.demodata.refresh:ds

Deploy admin feed:

   * Run on your local computer::
      fab target.integration app.demodata.demo_admin

Deploy main:

   * Run on your local computer::
      fab target.integration app.main.all

   * Ping admin feed::
      curl --data 'feed=http://rinfo-admin/feed/current' http://rinfo-main/collector

Verify main:

   * http://rinfo-main/feed/current
      should contain a feed with models etc.

   * http://rinfo-main/system/log/
      should contain entries for each source that has been collected

   You might need to ping main to collect data from the demo sources:

      * fab target.integration tools.ping_main_collector:http\://dv-demo/feed/current
      * fab target.integration tools.ping_main_collector:http\://sfs-demo/feed/current
      * fab target.integration tools.ping_main_collector:http\://prop-demo/feed/current
      * fab target.integration tools.ping_main_collector:http\://sou-demo/feed/current
      * fab target.integration tools.ping_main_collector:http\://ds-demo/feed/current

Deploy checker:

   * Run on your local computer::
      fab target.integration app.checker.all

Verify checker:

   * http://rinfo-checker/
      should contain the checker form. Put <http://dv-demo/feed/current> in
      the URL input field and click Check to check the feed. This should
      result in 10 green line being shown, one for each successfully checked
      feed entry.

Deploy service:

   * Run on your local computer::
      fab target.integration app.service.deploy_sesame
      fab target.integration app.service.all

   * Ping main feed::
      curl --data 'feed=http://rinfo-main/feed/current' http://rinfo-service/collector

Verify service:

   * http://rinfo-service/view/browse/publ
      should contain a main page from where you can browse the various
      collected data.

Upgrading the Integration Environment that has Existing Data
========================================================================

If you have done the initial setup and used the applications for a while
you might want to upgrade the environment but don't touch the data. To
upgrade your integration environment (tomcat and applications) please
follow the instructions below:

   * integration: sudo /etc/init.d/tomcat stop
   * integration: sudo rm -rf /opt/tomcat /opt/apache-tomcat-*
   * local: fab target.integration -R main fetch_tomcat_dist install_tomcat
   * local: fab target.integration app.demodata.dataset_war:dv app.demodata.dataset_war:sfs \
                app.demodata.dataset_war:prop app.demodata.dataset_war:sou app.demodata.dataset_war:ds
   * local: fab target.integration app.main.all
   * local: fab target.integration app.service.deploy_sesame
   * local: fab target.integration app.service.all
   * local: fab target.integration app.service.all

Deleting All Data
========================================================================

Sometimes you might want to start from scratch when it comes to the data.
This is an example of how to delete all data from the integration
environment. Run all of these commands on the integration environment::

   * sudo /etc/init.d/tomcat stop
   * sudo rm -rf /opt/rinfo/rdf/*
   * sudo rm -rf /opt/rinfo/store/*

If you also want to delete the demo data::

   * sudo rm -rf /opt/rinfo/demo-depots/*

Development Environment
========================================================================

For continuous integration see ``project/ci/README.txt``.

