load("versioning/svn.py")
load("deploy/rinfo_main.py")
load("deploy/rinfo_testsources.py")

config(
    project='rinfo',
    projectroot='..',
    base_data="$(projectroot)/resources/base",
    java_packages="$(projectroot)/packages/java",
)

