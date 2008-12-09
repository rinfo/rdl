load("versioning/svn.py")
load("deploy/envs.py")
load("deploy/deps.py")
load("deploy/rinfo_main.py")
load("deploy/rinfo_service.py")
load("deploy/rinfo_sesame_http.py")
load("deploy/rinfo_testsources.py")

config(
    project='rinfo',
    projectroot='..',
    base_data="$(projectroot)/resources/base",
    java_packages="$(projectroot)/packages/java",
)
