
##
# Local build

@requires('env', provided_by=sysenvs)
@depends(install_rinfo_pkg)
def package_service():
    local("cd $(java_packages)/rinfo-service/; mvn -P$(env) clean package")

##
# Server deploy

@requires('dist_dir', 'rinfo_dir', 'rinfo_rdf_repo_dir', provided_by=sysenvs)
@depends(service)
def setup_service():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir $(rinfo_dir)", fail='ignore')
    sudo("mkdir $(rinfo_rdf_repo_dir)", fail='ignore')

@depends(setup_service)
def deploy_service():
    deploy_war(
            "$(java_packages)/rinfo-service/target/rinfo-service-$(env).war",
            "rinfo-service")

@depends(package_service, deploy_service)
def service_all(): pass

##
# Sesame and Repo Util deploy

def package_sesame():
    pkgdir = "$(java_packages)/rinfo-sesame-http"
    local("cd %s; mvn package" % pkgdir)
    config.local_sesame_dir = "%s/target/dependency" % pkgdir

@depends(setup_service, package_sesame)
def deploy_sesame():
    for warname in ['openrdf-sesame', 'sesame-workbench']:
        deploy_war("$(local_sesame_dir)/%s.war" % warname, warname)

@depends(setup_service)
def service_repo_util():
    config.rinfo_repo_jar = "rinfo-rdf-repo-1.0-SNAPSHOT-jar-with-dependencies.jar"
    config.rinfo_service_props = "rinfo-service.properties"

@depends(service_repo_util)
def deploy_repo_util():
    local("cd $(java_packages)/rinfo-rdf-repo; mvn -P $(env) assembly:assembly")
    put("$(java_packages)/rinfo-service/src/environments/$(env)/$(rinfo_service_props)",
            "$(dist_dir)/")
    put("$(java_packages)/rinfo-rdf-repo/target/$(rinfo_repo_jar)", "$(dist_dir)/")

##
# Manage repository

@depends(service_repo_util)
def setup_repo():
    run("cd $(dist_dir); java -jar $(rinfo_repo_jar) setup $(rinfo_service_props) rinfo.service.repo")

@depends(service_repo_util)
def clean_repo():
    run("cd $(dist_dir); java -jar $(rinfo_repo_jar) setup $(rinfo_service_props) rinfo.service.repo")

