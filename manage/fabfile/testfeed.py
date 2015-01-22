from fabric.api import *
from fabfile.server import untar, create_path, restart_apache, take_ownership
from fabfile.sysconf import configure_sites
from fabfile.target import _needs_targetenv
from fabfile.util import get_value_from_password_store, PASSWORD_FILE_FTP_USERNAME_PARAM_NAME, \
    PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME, ftp_fetch


@roles('regression')
@task
def install_regression_source():
    _needs_targetenv()
    create_path(env.mgr_workdir)
    create_path(env.demo_data_root, use_sudo=True)
    take_ownership(env.demo_data_root, use_sudo=True)

    ftp_fetch(env.regression_compressed_file_name, env.testfeed_ftp_path, env.mgr_workdir,
              get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME),
              get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME))
    untar('%s/%s' % (env.mgr_workdir, env.regression_compressed_file_name) , env.demo_data_root)


@roles('skrapat')
@task
def install_skrapat_source():
    _needs_targetenv()
    create_path(env.mgr_workdir)
    create_path(env.demo_data_root, use_sudo=True)
    take_ownership(env.demo_data_root, use_sudo=True)

    ftp_fetch(env.skrapat_compressed_file_name, env.testfeed_ftp_path, env.mgr_workdir,
              get_value_from_password_store(PASSWORD_FILE_FTP_USERNAME_PARAM_NAME),
              get_value_from_password_store(PASSWORD_FILE_FTP_PASSWORD_PARAM_NAME))
    untar('%s/%s' % (env.mgr_workdir, env.skrapat_compressed_file_name), env.demo_data_root)


@roles('skrapat','regression')
@task
def install_sources():
    install_regression_source()
    install_skrapat_source()
    configure_sites()
    restart_apache()




