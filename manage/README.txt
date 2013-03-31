########################################################################
README - Management
########################################################################

Prerequisites
========================================================================

The RInfo project is managed using Fabric::

    http://www.fabfile.org/

See the Fabric website for installation instructions. Usually it's as easy as::

    $ pip install Fabric

Managing
========================================================================

To find out which commands are available, run:

    $ fab -l

Or, for a nice, nested view of the namespaces:

    $ fab -lFnested


Deployment
========================================================================

Version Control
------------------------------------------------------------------------

To make a *production* release, first cut a release properly.

The source code in Git is manager according to the principles of *git flow*: 
<https://github.com/nvie/gitflow>. The principal procedure is:

    $ git flow release start rel-$(date "+%Y%m%d")-1
    # ... bump application version numbers if necessary
    $ git flow release finish

Build and Deploy
------------------------------------------------------------------------

Make sure your code repository is clean and that you stand in the *master* 
branch (or preferably on a release tag)::

    $ git checkout master # or a release tag
    $ git status
    # On branch master
    nothing to commit (working directory clean)

Go to manage/ and run any or all of the following, depending on what to 
release.

* Deploy Admin data:

    $ fab target.prod app.admin.all

* Deploy Main:

    $ fab target.prod app.main.all

* Deploy Checker:

    $ fab target.prod app.checker.all

* Install ElasticSearch *unless already installed*:

    $ fab target.prod app.service.install_elasticsearch
    $ fab target.prod app.service.start_elasticsearch

* Deploy Sesame (should only update to new version if necessary):

    $ fab target.prod app.service.deploy_sesame

* Deploy Service:

    $ fab target.prod app.service.all

Ping Main to load Admin data immediately by running:

    $ fab target.prod app.admin.ping_main

* Update documentation:

    $ fab app.docs.build
    $ fab target.prod app.docs.deploy

* Update static files (e.g. index.html and robots.txt):

    $ fab target.prod -Rmain sysconf.sync_static_web


Working With An Environment from Scratch
========================================================================

Important: All instructions below pertain to the management of an Integration 
environment. But these instructions work pretty well for setting up a real
staging/demo (or production server) as well.

To do so, substitute target.demo (or target.prod) for target.integration below. 
You may also skip portions where applicable, such as for setting up servers and 
managing host names.

Set Up The Server
------------------------------------------------------------------------

(In some cases, scripts under /etc/init.d for starting and stopping the
tomcat and elasticsearch services fail when run on the target system
from fabric. In these cases, the alternative is to log into the target
system and manually running e.g. "sudo /etc/init.d/tomcat start".)

The integration environment is supposed to be run as a virtual server
on your local computer. It can host all rinfo applications. In the
instructions below it is assumed that local commands are run while
standing in the directory rinfo-trunk/manage

   * Setup a virtual server using a debian based linux distribution
   * Ensure that sudo is available (run: apt-get install sudo if not)
   * Ensure that sshd is available (run: sudo apt-get install openssh-server if not)
   * Ensure that you can ssh to the server, preferably passwordless
   * Ensure that the following tools are available: wget, curl, rsync, unzip
     and add-apt-repository (available in the ubuntu package
     python-software-properties)

   * Ensure that there is a package repository setup that contains
     ``sun-java6-jdk``:

      * For Debian::

         http://www.debian.org/doc/manuals/debian-java-faq/ch7.html ::
         $ sudo vim /etc/apt/sources.list ::
            deb <$HTTP-FTP...> $DIST main contrib non-free
         $ sudo apt-get update

      * For Ubuntu (10.04 Lucid)::

         $ sudo add-apt-repository "deb http://archive.canonical.com/ lucid partner"
         $ sudo apt-get update

      * For Ubuntu (10.10 Maverick Meerkat)::

         $ sudo add-apt-repository "deb http://archive.canonical.com/ maverick partner"
         $ sudo apt-get update

   * *For integration only*: edit the hosts file on your local computer so that
     it contains::

      <IP-OF-VIRTUAL-SERVER>  rinfo-integration
      <IP-OF-VIRTUAL-SERVER>  rinfo-main rinfo-service rinfo-admin rinfo-checker
      <IP-OF-VIRTUAL-SERVER>  sfs-demo dv-demo prop-demo sou-demo ds-demo

   * Create the rinfo user on your virtual server::

      $ adduser rinfo
      $ usermod -a -G sudo rinfo

   * Unless user and group tomcat:tomcat exist on the virtual server, create them::

      $ sudo useradd tomcat

   * Run on your local computer::

      $ fab target.integration -R main sysconf.install_server

   * .. Not needed, installing openjdk automatically.
     .. Previously: Run on your virtual server: ~/mgr_work/install/4_install-jdk.sh

   * Run on your local computer::

      $ fab target.integration -R admin,main,service,checker,demosource sysconf.configure_server server.reload_apache

The virtual server should now be ready for the deployment of the applications
and the demo data.

Create and Deploy Demo Data (only for local and integration environment)
------------------------------------------------------------------------

Deploy demo data from lagen.nu (very time consuming on first run, downloading
all the data):

   * Run on your local computer::

      $ fab target.integration -Rmain app.demodata.refresh:dv
      # (.. ?)
      $ fab target.integration -Rmain app.demodata.refresh:sfs
      # (.. ~ 12 min. to download and generate depot)

Deploy demo data from riksdagen.se (downloading may take a couple of hours):

   * Run on your local computer::

      $ fab target.integration -Rmain app.demodata.refresh:prop
      # (.. ~ 2 h. to download)
      $ fab target.integration -Rmain app.demodata.refresh:sou
      $ fab target.integration -Rmain app.demodata.refresh:ds

Deploy Admin Data
------------------------------------------------------------------------

To build the admin data configured with the *demo* dataset, run (on your local 
computer):

    $ fab app.admin.package:source=demo

This creates a directory hierarchy containing RDF and XHTML data,
based on the RDF files located in resources/base. This contains the
model (ontology), uri patterns, URIs for organizations and other
resources.

Deploy admin feed:

      $ fab target.integration app.admin.deploy

This rsync's the resulting directory hierarchy to the correct place in the
target environment (here integration).


Deploy The Applications
------------------------------------------------------------------------

Deploy main:

   * Run on your local computer::

      $ fab target.integration app.main.all

   * Ping admin feed::

      $ curl --data 'feed=http://rinfo-admin/feed/current' http://rinfo-main/collector

Verify main:

   * http://rinfo-main/feed/current

      should contain a feed with models etc.

   * http://rinfo-main/system/log/

      should contain entries for each source that has been collected

   You might need to ping main to collect data from the demo sources::

      $ fab target.integration app.tools.ping_main_collector:http\://dv-demo/feed/current
      $ fab target.integration app.tools.ping_main_collector:http\://sfs-demo/feed/current
      $ fab target.integration app.tools.ping_main_collector:http\://prop-demo/feed/current
      $ fab target.integration app.tools.ping_main_collector:http\://sou-demo/feed/current
      $ fab target.integration app.tools.ping_main_collector:http\://ds-demo/feed/current

Deploy checker:

   * Run on your local computer::

      $ fab target.integration app.checker.all

Verify checker:

   * http://rinfo-checker/

      should contain the checker form. Put <http://dv-demo/feed/current> in
      the URL input field and click Check to check the feed. This should
      result in 10 green line being shown, one for each successfully checked
      feed entry.

Deploy service:

   * Run on your local computer::

      $ fab target.integration app.service.deploy_sesame app.service.install_elasticsearch
      $ fab target.integration app.service.start_elasticsearch
      $ fab target.integration app.service.all

   * Ping main feed::

      $ curl --data 'feed=http://rinfo-main/feed/current' http://rinfo-service/collector

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
   * local: fab target.integration -R main install_tomcat
   * local: fab target.integration app.demodata.dataset_war:dv app.demodata.dataset_war:sfs \
                app.demodata.dataset_war:prop app.demodata.dataset_war:sou app.demodata.dataset_war:ds
   * local: fab target.integration app.main.all
   * local: fab target.integration app.service.deploy_sesame
   * local: fab target.integration app.service.all
   * local: fab target.integration app.service.all

Data Tools Bundled with the Web Apps
========================================================================

A script is provided to enable the running of certain diagnostic and
maintenance tools which are bundled with the deployed web applications.

These can be run on any deployed target server.

Regenerate The Main Atom Feed Archive
------------------------------------------------------------------------

To regenerate the atom feed archive from its contained entries, run::

    $ sudo -u tomcat ~/mgr_work/common/bin/run_webapp_tool.sh /opt/tomcat/webapps/rinfo-main/ se.lagrummet.rinfo.store.depot.FileDepotCmdTool rinfo-main-common.properties rinfo.depot index

This might take a couple of minutes for a depot of about 8 Gb in size (on a
commodity virtual server, as of 2011).

Regenerate The ElasticSearch Index
------------------------------------------------------------------------

To extract text data from elasticsearch, run::

    $ ~/mgr_work/common/bin/run_webapp_tool.sh /opt/tomcat/webapps/rinfo-service/ rinfo.service.cmd.ElasticTextExtract rinfo ~/elastic_text_extracts

Then delete the entire elasticsearch index by calling::

    $ curl -XDELETE http://localhost:9200/rinfo

Then regenerate the elasticsearch index from the Sesame repo combined with
extracted texts::

    $ ~/mgr_work/common/bin/run_webapp_tool.sh /opt/tomcat/webapps/rinfo-service/ rinfo.service.cmd.GenElastic rinfo-service.properties ~/elastic_text_extracts

(These usually complete in about 30 minutes. It's recommended to run these in
e.g. a screen session to ensure that loss of connection doesn't abort the
process.)

Deleting All Data
========================================================================

Sometimes you might want to start from scratch when it comes to the data.
This is an example of how to delete all data from the integration
environment. Run all of these commands on the integration environment::

   $ sudo /etc/init.d/tomcat stop
   $ sudo rm -rf /opt/rinfo/store/*

If you also want to delete the demo data::

   $ sudo rm -rf /opt/rinfo/demo-depots/*

To clear all data from the service backends, do:

   * local: curl -XPOST http://rinfo-integration:8080/sesame-workbench/repositories/rinfo/clear
   * integration: curl -XDELETE http://localhost:9200/rinfo

Development Environment
========================================================================

For details on manual steps when running a local setup (during development), 
see ``manage/running_rinfo_locally.txt``.

For continuous integration see ``project/ci/README.txt``.

