########################################################################
README - Continuous Integration
########################################################################

When developing its nice to have a continuous integration server. For 
this project we use Hudson. This is how Hudson is setup from scratch 
for this project if necessary.

# Login to the CI-server and create a hudson user

    $ ssh $CISERVER
    $ sudo adduser --system hudson
    $ sudo -u hudson wget http://hudson-ci.org/latest/hudson.war -O /home/hudson/hudson.war

# Install an init-script and start Hudson

    $ scp manage/project/ci/hudson $CISERVER:
    $ ssh $CISERVER
    $ sudo cp -vu hudson /etc/init.d/
    $ sudo chmod 0755 /etc/init.d/hudson
    $ sudo update-rc.d hudson defaults
    $ sudo /etc/init.d/hudson start

# Configure Apache

Put the following (or similar) config in the appropriate config file:

    # Hudson
    ProxyPass         /hudson  http://localhost:8080/hudson
    ProxyPassReverse  /hudson  http://localhost:8080/hudson
    ProxyRequests     Off
    
    # Local reverse proxy authorization override
    # Most unix distribution deny proxy by default (ie /etc/apache2/mods-enabled/proxy.conf in Ubuntu)
    <Proxy http://localhost:8080/hudson*>
      Order deny,allow
      Allow from all
    </Proxy>

References:

    * http://wiki.hudson-ci.org/display/HUDSON/Starting+and+Accessing+Hudson
    * http://wiki.hudson-ci.org/display/HUDSON/Running+Hudson+behind+Apache
    * http://wiki.hudson-ci.org/display/HUDSON/Securing+Hudson

# Configure Hudson

From within Hudson perform the following steps

    #. Install the "Hudson Cobertura plugin"
    #. Create a job for the project with the following settings
        #. It should build a maven2 project
        #. Subversion Repository URL: https://dev.lagrummet.se/svn/rinfo/trunk (you might have to create a new user)
        #. Root POM: packages/java/pom.xml
        #. Goals and options: clean install cobertura:cobertura
        #. Build -> Advanced… -> MAVEN_OPTS: -Xmx256m
        #. Activate the Post-build Actions: Publish Cobertura Coverage Report
        #. Cobertura xml report pattern: packages/java/**/target/site/cobertura/coverage.xml

# Auto deploy to demo environment

It is possible to make automatic deploys from the Hudson to another rinfo
environment. This is currently setup for the demo environment like this:

   #. Passwordless SSH has been setup so that the hudson user can SSH to
      demo.lagrummet.se as the rinfo user without needing a password.
      It is essential that a ~/.ssh/config file is created which states the
      user to use:

         host demo.lagrummet.se
            user rinfo

   #. The demo environment has been configured so that the rinfo user can
      restart tomcat without providing a password. This is done by appending
      to the sudoers file with visudo

         rinfo ALL=ALL, NOPASSWD: /etc/init.d/tomcat *

      which means that the rinfo user can run any command using sudo WITH
      a password but run /etc/init.d/tomcat with any arguments WITHOUT
      providing a password.

   #. The normal fabric scripts are used for the deploys but they are run
      with the headless parameter set to 1 like this:

         fab tg_demo main_all:headless=1
