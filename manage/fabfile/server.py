"""
Diagnostics and admin tasks
"""
from __future__ import with_statement
import contextlib
from fabric.contrib.project import os
import time
from fabric.api import *
from os.path import expanduser

from fabfile.util import install_public_key, role_is_active
from util import venv, get_value_from_password_store, PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME
from target import _needs_targetenv
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
    run("ls -latr %(dist_dir)s/" % env)


@task
def clean_dist():
    _needs_targetenv()
    run("rm -rf %(dist_dir)s/*" % env)


@task
def tail():
    _needs_targetenv()
    sudo("ls -t %(tomcat)s/logs/catalina*.* | head -1 | xargs tail -f" % env)


@task
def tail2():
    _needs_targetenv()
    run("tail -f %(tomcat)s/logs/localhost.%(datestamp)s.log" % env)


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
        " WEB-INF/classes/*.properties" % venv())


def ping_verify_roledef(report, role_name, url_add, content, name, class_name):
    if env.roledefs[role_name] is None:
        return
    if len(env.roledefs[role_name])==0:
        return
    url = "http://%s%s" % (env.roledefs[role_name][0], url_add)
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

    ping_verify_roledef(report, 'main', '/feed/current', 'tag:lagrummet.se,2009:rinfo', 'Verify main feed',
                        'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'service', '/ui/#/-/publ?q=a&_page=0&_pageSize=10', 'resultat',
                        'Verify service search', 'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'checker', '', 'RInfo Checker', 'Verify checker title',
                        'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'admin', '/feed/current.atom', 'RInfo Base Data', 'Verify admin feed',
                        'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'demosource', '/feed/current.atom', 'atom', 'Verify demosource feed',
                        'server.%(target)s.ping' % venv())
    ping_verify_roledef(report, 'lagrummet', '', 'lagrummet.se', 'Verify lagrummet search',
                        'server.%(target)s.ping' % venv())

    if not report.empty():
        report_name = "%(projectroot)s/ping_verify_%(target)s_report.log" % venv()
        report.create_report(report_name)
        print "Created report '%s'" % report_name


def tar(filename, target_path, command='czvf', test=False):
    with cd(target_path):
        if test:
            run('echo "Test file" > %s' % filename)
        else:
            run('tar %s %s *' % (command, filename))


def untar(filename, target_path, command='xzvf', use_sudo=False, test=False, is_local=False):
    tar_cmd = 'tar %s %s' % (command, filename)
    if test:
        print "Simulating tar command %s" % tar_cmd
        return

    if is_local:
        with lcd(target_path):
            local(tar_cmd)
    else:
        with cd(target_path):
            if use_sudo:
                sudo(tar_cmd)
            else:
                run(tar_cmd)


def ftp_push(filename, ftp_address, username, password, test=False):
    if test:
        print "ftp push %s to %s" % (filename, ftp_address)
    else:
        with hide('output','running'):
            run('curl -T %s %s --user %s:%s --ftp-create-dirs' % (filename, ftp_address, username, password))


def ftp_fetch(filename, ftp_address, target_path, username, password, test=False, is_local=False):
    if test:
        print "ftp fetch %s from %s to %s" % (filename, ftp_address, target_path)
        return

    cmd = 'curl %s/%s --user %s:%s --ftp-create-dirs -o %s' % (
        ftp_address, filename, username, password, filename)
    if is_local:
        with lcd(target_path):
            local(cmd)
    else:
        with cd(target_path):
            with hide('output','running'):
                run(cmd)



def tar_and_ftp_push(snapshot_name, name, password, source_tar_path, target_path, username, test=False):
    file_to_upload = '%s/%s.tar.gz' % (target_path, name)
    tar(file_to_upload, source_tar_path, test=test)
    ftp_push(file_to_upload, '%s/%s/%s.tar.gz' % (env.ftp_server_url, snapshot_name, name), username, password,
             test=test)


def ftp_fetch_and_untar(snapshot_name, name, tmp_path, target_tar_unpack_path, username, password, test=False, is_local=False):
    file_to_download = '%s.tar.gz' % name
    ftp_fetch(file_to_download, "%s/%s" % (env.ftp_server_url, snapshot_name), tmp_path, username, password, test=test, is_local=is_local)
    clean_path(target_tar_unpack_path, use_sudo=True, test=test, is_local=is_local)
    untar('%s/%s.tar.gz' % (tmp_path, name), target_tar_unpack_path, use_sudo=True, test=test, is_local=is_local)


@parallel
@roles('main')
def take_main_snapshot_and_push_to_ftp(snapshot_name, target_path, username, password, test=False):
    if not role_is_active('main'):
        return
    tar_and_ftp_push(snapshot_name, 'depot', password, '/opt/rinfo/store/', target_path, username, test=test)


@parallel
@roles('service')
def take_service_snapshot_and_push_to_ftp(snapshot_name, target_path, username, password, use_sesame=True,
                                          use_elasticsearch=True, test=False):
    if not role_is_active('service'):
        return
    if use_sesame:
        tar_and_ftp_push(snapshot_name, 'sesame', password, '/opt/rinfo/sesame-repo/', target_path, username,
                         test=test)
    if use_elasticsearch:
        tar_and_ftp_push(snapshot_name, 'elasticsearch', password, '/opt/elasticsearch/var/data/', target_path,
                         username, test=test)


def clean_path(tar_target_path, use_sudo=False, test=False, is_local=False):
    cmd = "rm -rf %s*" % tar_target_path
    if test:
        print "remove files %s" % tar_target_path
    elif is_local:
        local(cmd)
    elif use_sudo:
        sudo(cmd)
    else:
        run(cmd)


def create_path(target_path, test=False, use_sudo=False, is_local=False):
    if test:
        print "Make directory: %s" % target_path
        return
    cmd = "mkdir -p %s" % target_path
    if is_local:
        local(cmd)
    elif use_sudo:
        sudo(cmd)
    else:
        run(cmd)


def take_ownership(target_path, test=False, use_sudo=False):
    if test:
        print "take ownership of '%s'" % target_path
        return
    if use_sudo:
        sudo("chown %s:%s %s" % (env.user, env.user, target_path) )
    else:
        run("chown %s:%s %s" % (env.user, env.user, target_path))



def calculate_stored_or_new_snapshot_name(snapshot_name):
    #snapshot_name = name + "_" + datetime.datetime.now().strftime("%Y%m%d_%H%M%S")
    if not env.snapshot_name:
        env.snapshot_name = snapshot_name
    else:
        snapshot_name = env.snapshot_name
    return snapshot_name


@task
@roles('main', 'service')
def take_snapshot_and_push_to_ftp(name='snapshot', username='', password='', test=False, use_password_file=True):
    _needs_targetenv()

    tar_target_path = "%s/tmp/%s" % (env.mgr_workdir, name)
    snapshot_name = calculate_stored_or_new_snapshot_name(name)

    if use_password_file:
        username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, username)
        password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, password)

    if not test:
        tomcat_stop()
    clean_path(tar_target_path, test=test)
    create_path(tar_target_path, test=test)
    try:
        take_main_snapshot_and_push_to_ftp(snapshot_name, tar_target_path, username, password, test=test)
        take_service_snapshot_and_push_to_ftp(snapshot_name, tar_target_path, username, password, test=test)
    finally:
        clean_path(tar_target_path, test=test)
        if not test:
            tomcat_start()


@parallel
@roles('main')
def fetch_main_snapshot_from_ftp_and_install(snapshot_name, tar_target_path, username, password, test=False,
                                             is_local=False):
    if not role_is_active('main'):
        return
    ftp_fetch_and_untar(snapshot_name, 'depot', tar_target_path, '/opt/rinfo/store/', username, password, test=test,
                        is_local=is_local)


@parallel
@roles('service')
def fetch_service_snapshot_from_ftp_and_install(snapshot_name, tar_target_path, username, password, use_sesame=True,
                                                use_elasticsearch=True, test=False, is_local=False):
    if not role_is_active('service'):
        return
    if use_sesame:
        ftp_fetch_and_untar(snapshot_name, 'sesame', tar_target_path, '/opt/rinfo/sesame-repo/', username, password,
                            test=test, is_local=is_local)
    if use_elasticsearch:
        ftp_fetch_and_untar(snapshot_name, 'elasticsearch', tar_target_path, '/opt/elasticsearch/var/data/', username,
                            password, test=test, is_local=is_local)


@task
@roles('main', 'service')
def fetch_snapshot_from_ftp_and_install(name='snapshot' ,username='', password='', test=False, use_password_file=True):
    _needs_targetenv()

    tar_target_path = "%s/tmp/%s" % (env.mgr_workdir, name)
    snapshot_name = calculate_stored_or_new_snapshot_name(name)

    if use_password_file:
        username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, username)
        password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, password)

    is_local = env.target in ['dev_unix']

    print "is_local %s" % is_local

    if not test and not is_local:
        tomcat_stop()
    clean_path(tar_target_path, test=test, is_local=is_local)
    create_path(tar_target_path, test=test, is_local=is_local)
    try:
        fetch_main_snapshot_from_ftp_and_install(snapshot_name, tar_target_path, username, password, test=test,
                                                 is_local=is_local)
        fetch_service_snapshot_from_ftp_and_install(snapshot_name, tar_target_path, username, password, test=test,
                                                    is_local=is_local)
    finally:
        clean_path(tar_target_path, test=test, is_local=is_local)
        # todo empty varnish cache
        if not test and not is_local:
            tomcat_start()


def prefere_ipv4_to_speed_up_debian_updates():
    sudo('echo "precedence ::ffff:0:0/96  100" >>  /etc/gai.conf')


def prepare_sudo_for_debian_and_add_rinfo_user():
    stored_env_user = env.user
    env.user = 'root'
    run('apt-get update')
    run('apt-get install sudo -y')
    try:
        run('whoami')
        run('useradd %s -m -G sudo -s /bin/bash' % stored_env_user)
        run('passwd %s' % stored_env_user)
    except:
        run('usermod -a -G sudo %s' % stored_env_user)
        run('passwd %s' % stored_env_user)
    env.user = stored_env_user
    # Below sollution is NOT very impressive. To handle that fabric keeps session alive and does'nt update the
    # sudo group of the user. Therefore it cannot continue. Must restart.
    try:
        sudo('pwd') #test sudo works
    except :
        print 'Du to debian user group update issues within current ssh session, you need to restart same command again.'
        raise Exception()


@task
@roles('main', 'service', 'checker', 'admin', 'lagrummet', 'emfs', 'test', 'regression', 'skrapat', 'demosource')
def bootstrap():
    _needs_targetenv()
    #if not os_version() == 'Debian7':
    #    print 'Unsupported os version %%' % os_version()
    #    return
    prepare_sudo_for_debian_and_add_rinfo_user()
    prefere_ipv4_to_speed_up_debian_updates()

    install_public_key()
    if os.path.isfile('%s/.ssh/jenkins_id_rsa.pub' % expanduser('~')):
        install_public_key('jenkins_id_rsa.pub')


@task
@roles('main', 'service', 'checker', 'admin', 'lagrummet', 'emfs', 'test', 'regression', 'skrapat', 'demosource')
def os_version():
    output = run('cat /etc/*-release')
    if 'Debian' in output:
        if 'wheezy':
            return 'Debian7'
        if 'squeeze':
            return 'Debian6'
        if 'lenny':
            return 'Debian5'
    return 'Unknown'
