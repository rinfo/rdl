from fabric.api import *
from genericpath import exists


@task
def install_local_build_prerequisites():
    install_gvm()
    setup_groovy_version()
    setup_grails_version()


@task
def setup_groovy_version(version="2.4.0"):
    gvm_select("groovy", version)


@task
def setup_grails_version(version="2.4.3"):
    gvm_select("grails", version)


def install_gvm():
    if not exists("~/.gvm/bin/gvm-init.sh"):
        local("curl -s get.gvmtool.net | bash")
        local("source ~/.gvm/bin/gvm-init.sh")


def local_os_install(name):
    local("sudo apt-get install %s -y" % name)


def gvm_install(name, version):
    local("gvm i %s %s" % (name, version))


def gvm_use(name, version):
    local("gvm u %s %s"  % (name, version))


def gvm_is_version_installed(name, version):
    return local("gvm l %s"  % name, capture=True).find("* %s" % version) > -1   #> * 2.4.3


def gvm_make_sure_version_is_installed(name, version):
    if not gvm_is_version_installed(name, version):
        gvm_install(name, version)

def gvm_select(name, version):
    try:
        gvm_use(name, version)
    except:
        gvm_install(name, version)
        gvm_use(name, version)
