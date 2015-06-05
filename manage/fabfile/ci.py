from fabric.api import *
from fabric.contrib.files import exists, append
import os
from server import tar_and_ftp_push, clean_path, create_path
from util import get_value_from_password_store, PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, \
    PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, msg_sleep
from server import ftp_fetch_and_untar

BACKUP_FILE_LOCATION = "/var/lib/jenkins/thinbackup"
BACKUP_FTP_BACKUP_AREA = "jenkinsconfig_test"


@task
@roles('ci')
def bootstrap():
    server.bootstrap()


@task
@roles('ci')
def install():
    os_update()
    install_prerequisites()
    install_java()
    install_git()
    install_maven()
    install_fabric()
    install_phantomjs()
    install_casperjs()
    install_jenkins()


def install_prerequisites():
    os_install("curl")
    os_install("unzip")
    os_install("build-essential python-dev python-pkg-resources python-setuptools")


def install_git():
    os_install("git git-flow")


def install_java():
    os_install("openjdk-7-jre-headless openjdk-7-jdk")


def install_phantomjs():
    os_install("phantomjs")


def install_casperjs():
    sudo("wget https://github.com/n1k0/casperjs/zipball/1.1-beta3 > casperjs-1.1-beta3.zip")
    with cd("/usr/local"):
        sudo("unzip ~/casperjs-1.1-beta3.zip")
        sudo("ln -s /usr/local/n1k0-casperjs-4f105a9 casperjs")
        sudo("ln -s /usr/local/casperjs/bin/casperjs casperjs")


def install_fabric():
    os_install("python-lxml")
    sudo("easy_install pip")
    sudo("pip install fabric==1.8.2")
    sudo("pip install paramiko==1.10.1")



def install_jenkins():
    run("wget -q -O - https://jenkins-ci.org/debian/jenkins-ci.org.key | sudo apt-key add -")
    append("/etc/apt/sources.list.d/jenkins.list", "deb http://pkg.jenkins-ci.org/debian binary/", use_sudo=True)
    #sudo("sh -c 'echo deb http://pkg.jenkins-ci.org/debian binary/ > /etc/apt/sources.list.d/jenkins.list'")
    os_update()
    os_install("jenkins")
    msg_sleep(60, "for Jenkins to start")
    install_plugins()
    thin_backup_install()
    add_jenkins_user_group()
    put_config_file()
    restart()


def install_maven(version=""):
    os_install("maven%s" % version)


def os_install(name):
    sudo("apt-get install %s -y" % name)


def os_update():
    sudo("apt-get update")


def add_jenkins_user_group():
    sudo("sudo usermod -a -G shadow jenkins")


def put_config_file():
    put("%s/sysconf/%s/jenkins/config.xml" % (env.manageroot, env.target),"/var/lib/jenkins/.", use_sudo=True)


def restart():
    sudo("/etc/init.d/jenkins restart")


def jenkins_cli(command, param=None, name=None, deploy=True, restart_jenkins=False):
    if not exists("jenkins-cli.jar"):
        run("wget http://localhost:8080/jnlpJars/jenkins-cli.jar")  #http://oldci.lagrummet.se/jnlpJars/jenkins-cli.jar
    param_cmd = " %s" % param if param else ""
    name_cmd = " -name %s" % name if name else ""
    deploy_cmd = " -deploy" if deploy else ""
    restart_cmd = " -restart" if restart_jenkins else ""
    command_line = "java -jar jenkins-cli.jar -s http://localhost:8080/ %s%s%s%s " % (command, param_cmd, deploy_cmd, restart_cmd)
    #java -jar jenkins-cli.jar -s http://ci.lagrummet.se/ install-plugin SOURCE ... [-deploy] [-name VAL] [-restart]
    run(command_line)


def jenkins_cli_install_plugin(plugin_name):
    jenkins_cli("install-plugin", param=plugin_name)


def install_plugins():
    jenkins_cli_install_plugin("git")
    jenkins_cli_install_plugin("github")
    jenkins_cli_install_plugin("throttle-concurrents")
    jenkins_cli_install_plugin("parameterized-trigger")
    jenkins_cli_install_plugin("run-condition")
    jenkins_cli_install_plugin("text-finder")


def thin_backup_install():
    put("%s/sysconf/%s/jenkins/thinBackup.xml" % (env.manageroot, env.target),"/var/lib/jenkins/.", use_sudo=True)
    jenkins_cli_install_plugin("thinBackup")


@task
@roles('ci')
def thin_backup_copy_to_ftp():
    is_local = env.target == 'dev_unix'
    if is_local:
        if not os.path.exists(BACKUP_FILE_LOCATION):
            print "Nothing to backup. '/var/lib/jenkins/thinbackup/' is empty."
            return
    else:
        if not exists(BACKUP_FILE_LOCATION, use_sudo=True):
            print "Nothing to backup. '/var/lib/jenkins/thinbackup/' is empty."
            return
    username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, "rinfo")
    password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, "pwd")
    target_file_name = "FULL-%s" % env.datestamp
    tmp_thin_backup_tar_pack_path = "/tmp/thinbackup/"

    create_path(tmp_thin_backup_tar_pack_path, is_local=is_local)

    tar_and_ftp_push(BACKUP_FTP_BACKUP_AREA, target_file_name, password, BACKUP_FILE_LOCATION,
                     tmp_thin_backup_tar_pack_path, username, is_local=is_local)

    clean_path(tmp_thin_backup_tar_pack_path, is_local=is_local)


@task
@roles('ci')
def thin_backup_restore_from_ftp(backup_file):
    is_local = env.target == 'dev_unix'
    username = get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, "rinfo")
    password = get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, "pwd")
    tmp_thin_backup_tar_pack_path = "/tmp/thinbackup/"

    create_path(tmp_thin_backup_tar_pack_path, is_local=is_local)
    create_path(BACKUP_FILE_LOCATION, is_local=is_local)

    ftp_fetch_and_untar(BACKUP_FTP_BACKUP_AREA, backup_file, tmp_thin_backup_tar_pack_path,
                        BACKUP_FILE_LOCATION, username, password, is_local=is_local)

    clean_path(tmp_thin_backup_tar_pack_path, is_local=is_local)

    print "Backup files in place. You need to got to jenkins 'ThinBackup' under menu 'Manage' and press restore"