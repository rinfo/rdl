from fabric.api import *
from fabric.contrib.files import exists
from fabric.contrib.console import confirm
import re, os


@runs_once
def configure():
    env.hosts = [_get_svn_host()]
    env.bakuser = 'rinfo'
    env.bak_base = "/var/local/backups"
    env.svn_base = "/var/local/svn"
    env.trac_base = "/var/local/trac"
    env.project = 'rinfo'
    env.project_bak = "%(bak_base)s/%(project)s"%env
    env.source_trac = "%(trac_base)s/%(project)s"%env
    env.trac_hotcopy_dir = "%(project_bak)s/trac/trac.hotcopy"%env
    env.source_svn = "%(svn_base)s/%(project)s"%env
    env.dump_file = "%(project_bak)s/svn/svn.dump"%env
    env.bakfile = "rinfo-backups.tgz"

def _get_svn_host():
    return re.sub(r'.*Repository Root: \w+://([^/]+?)/.*', r'\1',
            local("svn info").replace('\n', ' '))

configure()

@runs_once
def _bakprep():
    require('project', provided_by=[configure])
    if not exists(env.bak_base):
        sudo("mkdir %(bak_base)s"%env)
    if not exists(env.project_bak):
        sudo("mkdir %(project_bak)s"%env)
        sudo("chown %(bakuser)s %(project_bak)s"%env)
        sudo("mkdir %(project_bak)s/{data,svn,trac}"%env, user=env.bakuser)

def bak_trac():
    _bakprep()
    if not exists(env.source_trac):
        abort("%(source_trac)s must exist and be a directory"%env)
    if exists(env.trac_hotcopy_dir):
        rmcmd = "rm -rf %(trac_hotcopy_dir)s"%env
        # making paranoid asserts to ensure a safe recursive rm!
        assert env.trac_hotcopy_dir.startswith(env.project_bak)
        assert rmcmd.endswith(env.trac_hotcopy_dir)
        # then run it:
        sudo(rmcmd, user=env.bakuser)
    sudo("trac-admin %(source_trac)s hotcopy %(trac_hotcopy_dir)s"%env)
    sudo("chown -R %(bakuser)s %(trac_hotcopy_dir)s"%env)

def bak_svn():
    _bakprep()
    if not exists(source_svn):
        abort("%(source_svn)s must exist and be a directory"%env)
    sudo("svnadmin dump %(source_svn)s > %(dump_file)s"%env, user=env.bakuser)

def bak():
    "Create backups of all project data."
    bak_trac()
    bak_svn()

def download_bak(todir='/tmp'):
    "Download packed tgz of backup data."
    require('project', provided_by=[configure])
    prompt('Specify download directory: ', 'todir',
            default=todir, validate=r'.*[^/]')
    if not os.path.exists(env.todir):
        abort("Local directory %(todir)s does not exist."%env)
    run("tar czvf %(bakfile)s %(project_bak)s/"%env)
    get("rinfo-backups.tgz", "%(todir)s/%(bakfile)s"%env)

