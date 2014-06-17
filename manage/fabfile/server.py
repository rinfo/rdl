"""
Diagnostics and admin tasks
"""
from __future__ import with_statement
import contextlib
import time
from fabric.api import *
from util import venv
from target import _needs_targetenv
from util import verify_url_content
from util import test_url
from util import JUnitReport


@task
def can_i_deploy():
    """Tests password less sudo for automatic deploys."""
    sudo("echo 'it seems that I can run sudo as '", shell=False)
    sudo("whoami", shell=False)

@task
def list_dist():
    _needs_targetenv()
    run("ls -latr %(dist_dir)s/"%env)

@task
def clean_dist():
    _needs_targetenv()
    run("rm -rf %(dist_dir)s/*"%env)

@task
def tail():
    _needs_targetenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f"%env)

@task
def tail2():
    _needs_targetenv()
    run("tail -f %(tomcat)s/logs/localhost.%(datestamp)s.log"%env)

@task
def restart_all():
    _needs_targetenv()
    sudo("/etc/init.d/apache2 stop")
    restart_tomcat()
    sudo("/etc/init.d/apache2 start")

@contextlib.contextmanager
def _managed_tomcat_restart(wait=5, headless=False, force_start=False):
    _needs_targetenv()
    with settings(warn_only=True):
        result = sudo("%(tomcat_stop)s" % env, shell=not headless)
    do_start = force_start or not result.failed
    yield
    if do_start:
        print "... restarting in",
        for i in range(wait, 0, -1):
            print "%d..." % i,
            time.sleep(1)
        print
        sudo("%(tomcat_start)s" % env, shell=not headless)

@task
def restart_tomcat():
    with _managed_tomcat_restart(force_start=True):
        pass

@task
def tomcat_stop():
    _needs_targetenv()
    sudo("/etc/init.d/tomcat stop")

@task
def tomcat_start():
    _needs_targetenv()
    sudo("/etc/init.d/tomcat start")

@task
def restart_apache():
    _needs_targetenv()
    #sudo("/etc/init.d/apache2 stop")
    #sudo("/etc/init.d/apache2 start")
    sudo("/etc/init.d/apache2 restart")

@task
def reload_apache():
    _needs_targetenv()
    sudo("/etc/init.d/apache2 graceful")

@task
def war_props(warname="ROOT"):
    _needs_targetenv()
    run("unzip -p %(tomcat_webapps)s/%(warname)s.war"
            " WEB-INF/classes/*.properties"%venv())

def ping_verify_roledef(report, role_name, url_add, content, name, class_name):
    if env.roledefs[role_name] is None:
        return
    url="http://%s%s" % (env.roledefs[role_name][0],url_add)
    #test_url(report, name % role_name, class_name % role_name, url, content)
    test_url(report, name, class_name, url, content)

@task
def ping_verify():
    _needs_targetenv()
    report = JUnitReport()
    ping_verify_roledef(report, 'main', '', '', 'Verify main responds', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'service', '', '', 'Verify service responds', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'checker', '', '', 'Verify checker responds', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'admin', '', '', 'Verify admin responds', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'demosource', '', '', 'Verify demosource responds', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'lagrummet', '', '', 'Verify lagrummet responds', 'server.%(target)s.ping' % venv())

    ping_verify_roledef(report, 'main', '/feed/current', 'tag:lagrummet.se,2009:rinfo', 'Verify main feed', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'service', '/ui/#/-/publ?q=a&_page=0&_pageSize=10', 'resultat', 'Verify service search', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'checker', '', 'RInfo Checker', 'Verify checker title', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'admin', '/feed/current.atom', 'RInfo Base Data', 'Verify admin feed', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'demosource', '/feed/current.atom', 'atom', 'Verify demosource feed', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'lagrummet', '', 'Hitta orden!', 'Verify lagrummet search', 'server.%(target)s.ping' % venv())

    if not report.empty():
        report_name = "%(projectroot)s/ping_verify_%(target)s_report.log" % venv()
        report.create_report(report_name)
        print "Created report '%s'" % report_name
