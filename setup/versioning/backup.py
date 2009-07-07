import sys; sys.path.append('.')
from usefab import *
from fabric.contrib.console import confirm
import re


@runs_once
def configure():
    env.hosts = [_get_svn_host()]
    env.svn_base = "/var/local/svn"
    env.trac_base = "/var/local/trac"
    env.bakuser = 'rinfo'
    env.bak_base = "/var/local/backups"
    env.project = 'rinfo'
    env.project_bak = v("${bak_base}/${project}")

def _get_svn_host():
    return re.sub(r'.*Repository Root: \w+://([^/]+?)/.*', r'\1',
            local("svn info").replace('\n', ' '))

configure()

@runs_once
def _bakprep():
    require('project', provided_by=[configure])
    if not exists(env.bak_base):
        sudo(v("mkdir ${bak_base}"))
    if not exists(env.project_bak):
        sudo(v("mkdir ${project_bak}"))
        sudo(v("chown ${bakuser} ${project_bak}"))
        sudo(v("mkdir ${project_bak}/{data,svn,trac}"), user=env.bakuser)

def bak_trac():
    _bakprep()
    source_trac = v("${trac_base}/${project}")
    if not exists(source_trac):
        abort(v("${source_trac} must exist and be a directory"))
    trac_hotcopy_dir = v("${project_bak}/trac/trac.hotcopy")
    if exists(trac_hotcopy_dir):
        rmcmd = v("rm -rf ${trac_hotcopy_dir}")
        # making paranoid asserts to ensure a safe recursive rm!
        assert trac_hotcopy_dir.startswith(env.project_bak)
        assert rmcmd.endswith(trac_hotcopy_dir)
        # then run it:
        sudo(rmcmd, user=env.bakuser)
    sudo(v("trac-admin ${source_trac} hotcopy ${trac_hotcopy_dir}"))
    sudo(v("chown -R ${bakuser} ${trac_hotcopy_dir}"))

def bak_svn():
    _bakprep()
    source_svn = v("${svn_base}/${project}")
    if not exists(source_svn):
        abort(v("${source_svn} must exist and be a directory"))
    dump_file = v("${project_bak}/svn/svn.dump")
    sudo(v("svnadmin dump ${source_svn} > ${dump_file}"), user=env.bakuser)

def bak():
    "Create backups of all project data."
    bak_trac()
    bak_svn()

def download_bak():
    "Download packed tgz of backup data."
    require('project', provided_by=[configure])
    prompt('Specify download directory: ', 'downdir',
            default='/tmp', validate=r'.*[^/]')
    if not exists(env.downdir):
        abort(v("Local directory ${downdir} does not exist."))
    bakfile = "rinfo-backups.tgz"
    run(v("tar czvf ${bakfile} ${project_bak}/"))
    get("rinfo-backups.tgz", v("${downdir}/${bakfile}"))

