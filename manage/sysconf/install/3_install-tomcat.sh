#!/bin/bash

version=$1
tomcat_user=$2
tomcat_group=$3
rinfo_user=$4
mgr_work_dir=$5

if [[ -f /opt/apache-tomcat-${version}/conf/server.xml ]] ; then
    echo 'Tomcat $version already installed with correct version. Aborting tomcat installation.'
    exit
fi

# Create the tomcat group and user (if they don't already exist)
id -g ${tomcat_group} > /dev/null 2>&1 || groupadd ${tomcat_group}
id ${tomcat_user} > /dev/null 2>&1 || useradd ${tomcat_user} -d /opt/tomcat/ -s /bin/false -r -g ${tomcat_group}

# Add the tomcat and the rinfo user to the tomcat group
usermod -a -G ${tomcat_group} ${tomcat_user}
usermod -a -G ${tomcat_group} ${rinfo_user}

echo Unpack tomcat
tar xzf apache-tomcat-${version}.tar.gz
echo Move tomcat to opt
mv apache-tomcat-${version} /opt/
echo backup server.xml to server.old.xml
mv /opt/apache-tomcat-${version}/conf/server.xml /opt/apache-tomcat-${version}/conf/server.old.xml
echo copy new server.xml file from ${mgr_work_dir}/install/server.xml to /opt/apache-tomcat-${version}/conf/server.xml
mv ${mgr_work_dir}/install/server.xml /opt/apache-tomcat-${version}/conf/server.xml
echo Completed this section
pushd /opt/
    test -h tomcat && rm tomcat
    ln -s apache-tomcat-${version} tomcat

    # Set owner to rinfo and group to tomcat
    chown -R ${rinfo_user}:${tomcat_group} apache-tomcat-${version}
    chown -h ${rinfo_user}:${tomcat_group} tomcat

    # Set permissions so that the group (tomcat) can write only to work, temp and logs directory
    # Inspired by: http://www.owasp.org/index.php/Securing_tomcat
    #chmod -R o= apache-tomcat-${version} #others cannot do anything
    #chmod -R g=r,+X apache-tomcat-${version} #tomcat can only read and execute executables and directories
    #chmod -R g+w apache-tomcat-${version}/work apache-tomcat-${version}/temp #tomcat can write only to work and temp
    #chmod -R g=wx apache-tomcat-${version}/logs #tomcat can write, but not read, to logs dir

    chmod -R 774 /opt/tomcat

popd

# Remove the unnecessary default applications
rm -rf /opt/tomcat/webapps/*


