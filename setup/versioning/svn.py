config(
    repo_base="https://source.verva.se/svn/rinfo",
    repo_tags="$(repo_base)/tags",
)

def tag_release(tag=None):
    config.tag = tag
    prompt('tag', "Tag", validate=r"^\w[a-z-_0-9\.]+-\d+(?:\.\d+)?$")
    msg = "Tagging $(tag)"
    # TODO: s/echo dummy-//
    local("echo dummy-svn copy $(repo_base)/trunk $(repo_tags)/$(tag) -m '%s'" % msg)

def list_tags():
    local("svn ls $(repo_tags)")

