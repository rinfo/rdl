#!/bin/bash
SCRIPT_DIR=$(dirname $0)

pushd /opt/tomcat
    chown -R tomcat webapps temp logs work conf
popd

# NOTE: do local rsync instead if this gets unwieldy
pushd $SCRIPT_DIR/../etc/
    cp -u init.d/tomcat /etc/init.d/
    chmod 0755 /etc/init.d/tomcat
    update-rc.d tomcat defaults

    cp -u apache2/workers.properties /etc/apache2/
    chown root:root /etc/apache2/workers.properties

    cp -u apache2/conf.d/jk.conf /etc/apache2/conf.d/
    chown root:root /etc/apache2/conf.d/jk.conf

    cp -u apache2/mods-available/proxy.conf /etc/apache2/mods-available/

    cp -u apache2/sites-available/default /etc/apache2/sites-available/
popd

pushd $SCRIPT_DIR/../www/
    cp -u *.* /var/www
popd
