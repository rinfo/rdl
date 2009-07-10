from fabric.api import *

env.repo_base = "https://dev.lagrummet.se/svn/rinfo"
env.repo_tags = "%(repo_base)s/tags"%env

TAG_PATTERN = r"^\w[a-z-_0-9\.]+-\d+(?:\.\d+)?(?:-\w+)$"

def tag_release(tag=None):
    env.tag = tag
    if not env.tag:
        prompt('Tag:', "tag", validate=TAG_PATTERN)
    env.msg = "Tagging %(tag)s"%env
    local("svn copy %(repo_base)s/trunk %(repo_tags)s/%(tag)s -m '%(msg)s'"%env, capture=False)

def list_tags():
    local("svn ls %(repo_tags)s"%env, capture=False)

