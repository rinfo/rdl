##
# Sesame HTTP Server deploy

@requires('env', provided_by=[dev_unix, staging, production])
def deploy_sesame_http():
    # Prepare parameters
    pkg_dir = "../packages/java/rinfo-sesame-http"
    import ConfigParser    
    cf = ConfigParser.ConfigParser()
    cf.read("%s/version.properties" % pkg_dir)
    proj_name = cf.get("main", "rinfo.sesame.http.version")
    war_name = cf.get("main", "rinfo.sesame.http.war.name")
    war_dir = "%s/target/%s-$(env).dir/%s/lib" % (pkg_dir, proj_name, proj_name)    
    war_file = "%s/%s.war" % (war_dir, war_name)    
    dest_file = "$(tomat_webapps)/%s.war" % war_name
    # Assemble war file
    local("cd %s; mvn -P $(env) assembly:directory" % pkg_dir)
    # Clean old installs
    run("rm -rf $(tomat_webapps)/%s.war" % war_name, fail='warn')
    run("rm -rf $(tomat_webapps)/%s" % war_name, fail='warn')
    # Deploy war file
    put(war_file, dest_file, fail='warn')

##
# Clean repository

def clean_repo():
    local("rm -rf /opt/_workapps/rinfo/aduna", fail='warn')
    local("mkdir /opt/_workapps/rinfo/aduna", fail='warn')

##
# Setup empty repository

#TODO: def setup_repo():
# java RepositoryUtil setup
# check exit code

#TODO: def setup_tomcat():

#TODO: def setup_tomcat_service():
