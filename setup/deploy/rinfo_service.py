
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
            "$(java_packages)/rinfo-service/target/rinfo-service-1.0-SNAPSHOT.war",
            "rinfo-service")

@depends(package_service, deploy_service)
def service_all(): pass

##
# Sesame and Repo Util deploy

@depends(setup_service)
def deploy_sesame():
    deploy_war(
            "$(java_packages)/rinfo-sesame-http/lib/openrdf-sesame.war",
            "openrdf-sesame")

@depends(setup_service)
def service_with_repo():
    config.rinfo_repo_jar = "rinfo-rdf-repo-1.0-SNAPSHOT-jar-with-dependencies.jar"

@depends(service_with_repo)
def deploy_repo_util():
    local("cd $(java_packages)/rinfo-rdf-repo; mvn -P $(env) assembly:assembly")
    put("$(java_packages)/rinfo-rdf-repo/target/$(rinfo_repo_jar)", "$(dist_dir)/$(rinfo_repo_jar)")

##
# Manage repository

@depends(service_with_repo)
def setup_repo():
    run("cd $(dist_dir); java -jar rinfo-rdf-repo-1.0-SNAPSHOT.jar setup remote")

@depends(service_with_repo)
def clean_repo():
    run("cd $(dist_dir); java -jar rinfo-rdf-repo-1.0-SNAPSHOT.jar clean remote")

