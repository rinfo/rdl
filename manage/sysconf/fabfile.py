load("../deploy/envs.py")

config(
    sysconf_dir=".",
)

##
# Tomcat configuration
# See: <http://tomcat.apache.org/connectors-doc-archive/jk2/proxy.html>

# TODO: consider splitting method into several env dependent versions, instead
# of this joint method for both dev_unix and staging.

@requires('sysconf_dir', 'dist_dir', 'tomcat')
def configure_tomcat():
    sudo("$(tomcat_stop)", fail='warn')
    run("rm -rf $(dist_dir)/tomcat", fail='warn')
    run("mkdir $(dist_dir)/tomcat", fail='warn')
    put("$(sysconf_dir)/tomcat/setenv.sh", "$(dist_dir)/tomcat/setenv.sh", fail='ignore')
    put("$(sysconf_dir)/tomcat/tomcat6.conf", "$(dist_dir)/tomcat/tomcat6.conf", fail='ignore')
    sudo("cp $(dist_dir)/tomcat/setenv.sh $(tomcat)/bin/setenv.sh", fail='ignore')
    sudo("cp $(dist_dir)/tomcat/tomcat6.conf $(tomcat)/conf/tomcat6.conf", fail='ignore')
    sudo("$(tomcat_start)")

##
# Apache configuration
# See: <http://httpd.apache.org/docs/2.0/vhosts/name-based.html>
# See: <http://httpd.apache.org/docs/2.0/mod/core.html#namevirtualhost>
# See: <http://httpd.apache.org/docs/2.0/mod/mod_proxy.html#access>

@requires('sysconf_dir', 'dist_dir', 'apache')
def configure_apache():
    run("rm -rf $(dist_dir)/apache", fail='warn')
    run("mkdir $(dist_dir)/apache", fail='warn')
    sudo("mkdir /etc/apache2/rinfo_conf", fail='ignore')
    put("$(sysconf_dir)/apache/apache2", "$(dist_dir)/apache/apache2", fail='warn')
    put("$(sysconf_dir)/apache/rinfo-vhost.conf", "$(dist_dir)/apache/rinfo-vhost.conf", fail='warn')
    sudo("cp $(dist_dir)/apache/rinfo-vhost.conf /etc/apache2/rinfo_conf/rinfo-vhost.conf", fail='warn')
    sudo("cp $(dist_dir)/apache/apache2 /etc/sysconfig/apache2", fail='warn')
    sudo("chown root:root /etc/apache2/rinfo_conf/rinfo-vhost.conf", fail='warn')
    sudo("chown root:root /etc/sysconfig/apache2", fail='warn')
    sudo("rcapache2 restart", fail='ignore')

