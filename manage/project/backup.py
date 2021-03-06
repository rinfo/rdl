from fabric.api import *
from fabric.contrib.files import exists
from fabric.contrib.console import confirm
import re, os


@runs_once
def configure():
    # env.hosts = [_get_svn_host()]
    env.hosts = ['dev.lagrummet.se']
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
    env.bak_file = "rinfo-backups.tgz"
    env.bak_path = "%(bak_base)s/%(bak_file)s"%env

def _get_svn_host():
    for l in local("svn info --xml").splitlines():
        for svnhost in re.findall(r'\s*<root>\w+://([^/]+?)/.*</root>', l):
            return svnhost

configure()

@runs_once
def _bakprep():
    require('project', provided_by=[configure])
    if not exists(env.bak_base):
        sudo("mkdir %(bak_base)s"%env)
    if not exists(env.project_bak):
        sudo("mkdir %(project_bak)s"%env)
        sudo("chown %(bakuser)s %(project_bak)s"%env)
        _budo("mkdir %(project_bak)s/{data,svn,trac}"%env)

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
        _budo(rmcmd)
    sudo("trac-admin %(source_trac)s hotcopy %(trac_hotcopy_dir)s"%env)
    sudo("chown -R %(bakuser)s %(trac_hotcopy_dir)s"%env)

def bak_svn():
    _bakprep()
    if not exists(env.source_svn):
        abort("%(source_svn)s must exist and be a directory"%env)
    _budo("svnadmin dump %(source_svn)s > %(dump_file)s"%env)

def bak_admin_log():
    _budo("cp /config.txt %(project_bak)s/"%env)

def bak():
    "Create backups of all project data."
    
    bak_trac()
    bak_svn()
    bak_admin_log()
    
    if exists(env.bak_path):
        _budo("rm %(bak_path)s"%env)
    
    _budo("tar czvf %(bak_path)s %(project_bak)s/"%env)

def download_bak(todir='/tmp'):
    "Download packed tgz of backup data."
    require('project', provided_by=[configure])
    prompt('Specify download directory: ', 'todir',
            default=todir, validate=r'.*[^/]')
    if not os.path.exists(env.todir):
        abort("Local directory %(todir)s does not exist."%env)
    get(env.bak_path, "%(todir)s/%(bak_file)s"%env)

def _budo(cmd): sudo(cmd, user=env.bakuser)

