##
# Sesame HTTP Server deploy

@requires('env', provided_by=[dev_unix, staging, production])
def deploy_sesame_http():
    # Assemble war file
    pkg_dir = "../packages/java/rinfo-sesame-http"
    local("cd %s; mvn -P $(env) assembly:directory" % pkg_dir)
    # Prepare parameters
    import ConfigParser    
    cf = ConfigParser.ConfigParser()
    cf.read("%s/target/classes/version.properties" % pkg_dir)
    proj_name = cf.get("main", "rinfo.sesame.http.version")
    war_name = cf.get("main", "rinfo.sesame.http.war.name")
    war_dir = "%s/target/%s-$(env).dir/%s/lib" % (pkg_dir, proj_name, proj_name)    
    war_file = "%s/%s.war" % (war_dir, war_name)    
    dest_file = "$(tomat_webapps)/%s.war" % war_name
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

@requires('env', provided_by=[dev_unix, staging, production])
def setup_sesame_http_repo():
    pkg_dir = "../packages/java/rinfo-rdf-repo"
    local("cd %s; mvn -P $(env) assembly:assembly" % pkg_dir)
    import ConfigParser    
    cf = ConfigParser.ConfigParser()
    cf.read("%s/target/classes/version.properties" % pkg_dir)
    proj_name = cf.get("main", "rinfo.rdf.repo.version")
    zip_file = "%s/target/%s-with-dependencies.zip" % (pkg_dir, proj_name)
    remote_file_name = "%s-$(fab_timestamp)" % proj_name
    run("mkdir $(dist_dir)", fail='ignore')        
    put(zip_file, "$(dist_dir)/%s.zip" % remote_file_name)
    run("cd $(dist_dir); unzip %s.zip" % remote_file_name)
    run("cd $(dist_dir); mv %s %s" % (proj_name, remote_file_name))
    run("cd $(dist_dir)/%s; java -jar %s.jar setup remote" % (remote_file_name, proj_name))
    

#TODO: def setup_tomcat():

#TODO: def setup_tomcat_service():
