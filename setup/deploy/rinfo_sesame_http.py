##
# Sesame HTTP Server deploy

_needs_sesame_http_env = requires(
        'env', 'host_map', 'dist_dir', 'rinfo_dir', 'rinfo_rdf_repo_dir', 'tomcat',
        provided_by=[dev_unix, staging, production])

@_needs_sesame_http_env
def setup_sesame_http():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir $(rinfo_dir)", fail='ignore')
    sudo("mkdir $(rinfo_rdf_repo_dir)", fail='ignore')

@depends(setup_sesame_http)
def prepare_deploy_sesame_http():
    pkg_dir = "../packages/java/rinfo-sesame-http"
    local("cd %s; mvn -P $(env) clean assembly:directory" % pkg_dir)
    import ConfigParser    
    cf = ConfigParser.ConfigParser()
    cf.read("%s/target/classes/version.properties" % pkg_dir)
    proj_name = cf.get("main", "rinfo.sesame.http.version")
    war_name = cf.get("main", "rinfo.sesame.http.war.name")
    war_dir = "%s/target/%s-$(env).dir/%s/lib" % (pkg_dir, proj_name, proj_name)
    conf_dir = "%s/target/%s-$(env).dir/%s/conf" % (pkg_dir, proj_name, proj_name)
    config(
        sesame_http_war_name=war_name,
        sesame_http_war_file="%s/%s.war" % (war_dir, war_name),
        tomcat_conf_dir=conf_dir,
    )

@depends(prepare_deploy_sesame_http)
def deploy_sesame_http():
    dest_war = "$(dist_dir)/$(sesame_http_war_name).war"
    put("$(sesame_http_war_file)", dest_war, fail='warn')
    sudo("$(tomcat_stop)", fail='warn')
    sudo("rm -rf $(tomcat_webapps)/$(sesame_http_war_name).war", fail='warn')
    sudo("rm -rf $(tomcat_webapps)/$(sesame_http_war_name)", fail='warn')
    sudo("mv $(dist_dir)/$(sesame_http_war_name).war $(tomcat_webapps)/$(sesame_http_war_name).war")
    sudo("$(tomcat_start)")

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

##
# Tomcat configuration

# TODO: consider splitting method into several env dependent versions, instead 
# of this joint method for both dev_unix and staging.

@depends(prepare_deploy_sesame_http)
def configure_tomcat():
    sudo("$(tomcat_stop)", fail='warn')    
    run("rm -rf $(dist_dir)/tomcat", fail='warn')
    run("mkdir $(dist_dir)/tomcat", fail='warn')
    put("$(tomcat_conf_dir)/setenv.sh", "$(dist_dir)/tomcat/setenv.sh", fail='ignore')
    put("$(tomcat_conf_dir)/server.xml", "$(dist_dir)/tomcat/server.xml", fail='warn')
    put("$(tomcat_conf_dir)/tomcat6.conf", "$(dist_dir)/tomcat/tomcat6.conf", fail='ignore')
    sudo("rm -rf $(tomcat)/bin/setenv.sh", fail='ignore')
    sudo("rm -rf $(tomcat)/conf/server.xml", fail='warn')    
    sudo("cp $(dist_dir)/tomcat/setenv.sh $(tomcat)/bin/setenv.sh", fail='ignore')
    sudo("cp $(dist_dir)/tomcat/server.xml $(tomcat)/conf/server.xml", fail='warn')
    sudo("cp $(dist_dir)/tomcat/tomcat6.conf $(tomcat)/conf/tomcat6.conf", fail='warn')
    sudo("$(tomcat_start)")
