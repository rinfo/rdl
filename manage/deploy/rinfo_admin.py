from fabric.api import env, local, roles
from fabric.contrib.project import rsync_project
from util import slashed
from targetenvs import _needs_targetenv


env.adminbuild = '%(builddir)s/rinfo-admin'%env

def package_admin():
    local("cd %(toolsdir)s/rinfomain &&"
        "groovy base_as_feed.groovy -b ../../resources/base/"
            " -o %(adminbuild)s"%env)

@roles('admin')
def deploy_admin():
    _needs_targetenv()
    rsync_project((env.admin_webroot), slashed(env.adminbuild),
            exclude=".*", delete=True)

def ping_main_with_admin():
    feed_url = "http://%s/admin/feed/current" % env.roledefs['main'][0]
    collector_url = "http://%s/collector/" % env.roledefs['admin'][0]
    print local("curl --data 'feed=%(feed_url)s' %(collector_url)s"%vars())


