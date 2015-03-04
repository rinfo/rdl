"""
Target Environments
"""
from fabric.api import *
from fabfile.util import get_value_from_password_store, PASSWORD_FILE_STANDARD_PASSWORD_PARAM_NAME, \
    PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, PASSWORD_FILE_DB_USERNAME_PARAM_NAME, \
    PASSWORD_FILE_DB_PASSWORD_PARAM_NAME


targetenvs = []


def targetenv(f):
    """
    Decorator function that makes sure that the list targetenvs contains all
    available target environments. It does so by adding the decorated function
    to the targetenvs list which is used by the _needs_targetenv function.
    """
    targetenvs.append("target." + f.__name__)
    return task(f)


def _needs_targetenv():
    """
    Makes sure that the env dictionary contains a certain set of keys. These
    keys are provided by one of the targetenv functions (decorated with
    @targetenv). Targets calling this function require a target to have been
    invoked.
    """
    require('target', 'roledefs', 'dist_dir', 'tomcat', provided_by=targetenvs)


@targetenv
def dev_unix():
    """Set target env to: dev_unix"""
    # Name env:
    env.target = "dev_unix"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['localhost'],
        'service': ['localhost'],
        'checker': ['localhost'],
        'admin': ['localhost'],
        'demosource': ['localhost'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def demo():
    """Set target env to: demo"""
    # Name env:
    env.target = "demo"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.demo.lagrummet.se']
    env.roledefs['service'] = ['service.demo.lagrummet.se']
    env.roledefs['checker'] = ['checker.demo.lagrummet.se']
    env.roledefs['admin'] = ['admin.demo.lagrummet.se']
    env.roledefs['lagrummet'] = ['demo.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def test():
    """Set target env to: test"""
    # Name env:
    env.target = "test"
    # Machines:
    env.user = 'rinfo'
    env.roledefs['main'] = ['rinfo.test.lagrummet.se']
    env.roledefs['service'] = ['service.test.lagrummet.se']
    env.roledefs['checker'] = ['checker.test.lagrummet.se']
    env.roledefs['admin'] = ['admin.test.lagrummet.se']
    env.roledefs['demosource'] = ['testfeed.lagrummet.se']
    env.roledefs['lagrummet'] = ['test.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()

@targetenv
def dom():
    """Set target env to: test"""
    # Name env:
    env.target = "dom"
    # Machines:
    env.user = 'rinfo'
    
    env.roledefs['main'] = ['rinfo.t1.lagr.dev.dom.se']
    env.roledefs['service'] = ['service.t1.lagr.dev.dom.se']
    env.roledefs['checker'] = ['checker.t1.lagr.dev.dom.se']
    env.roledefs['admin'] = ['admin.t1.lagr.dev.dom.se']
    env.roledefs['lagrummet'] = ['t1.lagr.dev.dom.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def ville():
    """Set target env to: test"""
    # Name env:
    env.target = "ville"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.ville.lagrummet.se']
    env.roledefs['service'] = ['service.ville.lagrummet.se']
    env.roledefs['checker'] = ['checker.ville.lagrummet.se']
    env.roledefs['admin'] = ['admin.ville.lagrummet.se']
    env.roledefs['lagrummet'] = ['ville.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def valle():
    """Set target env to: test"""
    # Name env:
    env.target = "valle"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.valle.lagrummet.se']
    env.roledefs['service'] = ['service.valle.lagrummet.se']
    env.roledefs['checker'] = ['checker.valle.lagrummet.se']
    env.roledefs['admin'] = ['admin.valle.lagrummet.se']
    env.roledefs['lagrummet'] = ['valle.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()

@targetenv
def viktor():
    """Set target env to: test"""
    # Name env:
    env.target = "viktor"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.viktor.lagrummet.se']
    env.roledefs['service'] = ['service.viktor.lagrummet.se']
    env.roledefs['checker'] = ['checker.viktor.lagrummet.se']
    env.roledefs['admin'] = ['admin.viktor.lagrummet.se']
    env.roledefs['lagrummet'] = ['viktor.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()

@targetenv
def stage():
    """Set target env to: stage"""
    # Name env:
    env.target = "stage"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.stage.lagrummet.se']
    env.roledefs['service'] = ['service.stage.lagrummet.se']
    env.roledefs['checker'] = ['checker.stage.lagrummet.se']
    env.roledefs['admin'] = ['admin.stage.lagrummet.se']
    env.roledefs['lagrummet'] = ['stage.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = "127.0.0.1"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def regression():
    """Set target env to: regression"""
    # Name env:
    env.target = "regression"
    # Machines:
    env.user = 'rinfo'

    env.roledefs['main'] = ['rinfo.regression.lagrummet.se']
    env.roledefs['service'] = ['service.regression.lagrummet.se']
    env.roledefs['checker'] = ['checker.regression.lagrummet.se']
    env.roledefs['admin'] = ['admin.regression.lagrummet.se']
    env.roledefs['lagrummet'] = ['regression.lagrummet.se']

    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def beta():
    """Set target env to: beta"""
    # Name env:
    env.target = "beta"
    # Machines:
    env.user = 'rinfo'
    env.roledefs['main'] = ['rinfo.beta.lagrummet.se']
    env.roledefs['service'] = ['service.beta.lagrummet.se']
    env.roledefs['checker'] = ['checker.beta.lagrummet.se']
    env.roledefs['admin'] = ['admin.beta.lagrummet.se']
    env.roledefs['lagrummet'] = ['beta.lagrummet.se']
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Varnish, if installing distributed make sure listen_ip_varnish is empty (listen to all interfaces)
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = ""
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def skrapat():
    """Set target env to: Skrapat"""
    # Name env:
    env.target = "skrapat"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo.skrapat.lagrummet.se'],
        'service': ['service.skrapat.lagrummet.se'],
        'checker': ['checker.skrapat.lagrummet.se'],
        'admin': ['admin.skrapat.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
        'lagrummet': ['skrapat.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def testfeed():
    """Set target env to: env

       To work, you must set correct host(env.roledefs) values in /etc/hosts.
    """
    # Name env:
    env.target = "testfeed"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['main.testfeed'],
        'service': ['service.testfeed'],
        'checker': ['checker.testfeed'],
        'admin': ['admin.testfeed'],
        'demosource': ['testfeed.lagrummet.se'],
        'regression': ['regression.testfeed.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['emfs'],
        'regression': ['regression'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def scraped():
    """Set target env to: demo"""
    # Name env:
    env.target = "scraped"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['testfeed.lagrummet.se'],
        'service': ['testfeed.lagrummet.se'],
        'checker': ['testfeed.lagrummet.se'],
        'admin': ['testfeed.lagrummet.se'],
        'demosource': ['testfeed.lagrummet.se'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


# Integration is a virtual environment that you could setup on your own computer
# See README.txt for more information
@targetenv
def integration():
    """Set target env to: integration"""
    # Name env:
    env.target = "integration"
    # Machines:
    env.user = 'rinfo'
    env.roledefs = {
        'main': ['rinfo-main'],
        'service': ['rinfo-service'],
        'checker': ['rinfo-checker'],
        'doc': ['rinfo-integration'],
        'admin': ['rinfo-integration'],
        'demosource': ['rinfo-integration'],
    }
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'demosource': ['sfs', 'dv', 'prop', 'sou', 'ds'],
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def prod():
    """Set target env to: prod"""
    # Name env:
    env.target = "prod"
    # Machines:
    env.user = 'rinfo'
    env.roledefs['main'] = ['rinfo.lagrummet.se']
    env.roledefs['service'] = ['service.lagrummet.se']
    env.roledefs['checker'] = ['checker.lagrummet.se']
    env.roledefs['doc'] = ['dev.lagrummet.se']
    env.roledefs['admin'] = ['admin.lagrummet.se']
    env.roledefs['lagrummet'] = ['www.lagrummet.se']
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    # Varnish
    env.workdir_varnish = "/opt/varnish"
    env.listen_ip_varnish = ""
    # Apache
    env.admin_webroot = "/var/www/admin"
    env.docs_webroot = "/var/www/dokumentation"
    env.apache_sites = {
        'main': ['rinfo-main', 'admin'],
        'service': ['service'],
        'checker': ['checker'],
    }
    # Tomcat
    _tomcat_env()
    _initialize_password()


@targetenv
def infrastructure():
    env.target = "infrastructure"
    # Machines:
    env.user = 'rinfo'
    env.roledefs['emfs'] = ['testfeed.lagrummet.se']
    env.roledefs['test'] = ['testfeed.lagrummet.se']
    env.roledefs['regression'] = ['testfeed.lagrummet.se']
    env.roledefs['skrapat'] = ['testfeed.lagrummet.se']
    env.roledefs['demosource'] = ['testfeed.lagrummet.se']
    # Manage
    env.mgr_workdir = "/home/%(user)s/mgr_work" % env
    env.dist_dir = 'rinfo_dist'
    # Filesystem paths
    env.rinfo_dir = '/opt/rinfo'
    env.rinfo_main_store = "/opt/rinfo/store"
    env.rinfo_rdf_repo_dir = '/opt/rinfo/sesame-repo'
    env.demo_data_root = "/opt/rinfo/demo-depots"
    env.testfeed_ftp_path = '%s/testfeed' % env.ftp_server_url
    env.regression_compressed_file_name = 'regression.tgz'
    env.skrapat_compressed_file_name = 'scraped.tgz'
    # Apache
    env.apache_sites = {
        'test': ['test'],
        'emfs': ['emfs'],
        'demosource': ['emfs','sfs', 'dv', 'prop', 'sou', 'ds', 'va', 'regression'],
        'regression': ['regression'],
        'skrapat': ['skrapat'],
        }
    # Tomcat
    _tomcat_env()
    _initialize_password()


def _tomcat_env():
    env.apache_jk_tomcat = True
    # when change version of tomcat, must check server.xml (../../sysconf/common/tomcat/server.xml)
    env.tomcat_version = "7.0.57"
    env.tomcat = "/opt/tomcat"
    env.tomcat_webapps = "%(tomcat)s/webapps" % env
    env.tomcat_start = '/etc/init.d/tomcat start'
    env.tomcat_stop = '/etc/init.d/tomcat stop'
    env.tomcat_user = 'tomcat'
    env.tomcat_group = 'tomcat'


def _initialize_password():
    env.password = get_value_from_password_store(PASSWORD_FILE_STANDARD_PASSWORD_PARAM_NAME,env.password)
