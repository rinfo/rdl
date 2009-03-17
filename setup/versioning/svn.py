config(
    repo_base="https://dev.lagrummet.se/svn/rinfo",
    repo_tags="$(repo_base)/tags",
)

TAG_PATTERN = r"^\w[a-z-_0-9\.]+-\d+(?:\.\d+)?(?:-\w+)$"

def tag_release(tag=None):
    config.tag = tag
    prompt('tag', "Tag", validate=TAG_PATTERN)
    msg = "Tagging $(tag)"
    local("svn copy $(repo_base)/trunk $(repo_tags)/$(tag) -m '%s'" % msg)

def list_tags():
    local("svn ls $(repo_tags)")

