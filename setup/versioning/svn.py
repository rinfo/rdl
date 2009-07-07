import sys; sys.path.append('.')
from usefab import *


env.repo_base = "https://dev.lagrummet.se/svn/rinfo"
env.repo_tags = v("${repo_base}/tags")

TAG_PATTERN = r"^\w[a-z-_0-9\.]+-\d+(?:\.\d+)?(?:-\w+)$"

def tag_release(tag=None):
    env.tag = tag
    prompt('tag', "Tag", validate=TAG_PATTERN)
    msg = v("Tagging ${tag}")
    local(v("svn copy ${repo_base}/trunk ${repo_tags}/${tag} -m '${msg}'"), capture=False)

def list_tags():
    local(v("svn ls ${repo_tags}"), capture=False)

