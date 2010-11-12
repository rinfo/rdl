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

In order to make automatic deploys to some other environment you should use 
the normal fabric scripts. The scripts needs to be able to run without manual 
input. 

To achieve this you need to make the Hudson user able to login without 
using a password on the demo environment using password less SSH. It is also 
necessary to configure the demo environment so that the Hudson user can sudo 
without entering a password. You achieve this by editing the sudoers file with 
"sudo visudo" and add the following line:

   hudson_user ALL=(ALL) NOPASSWD: ALL

where "hudson_user" should be exchanged to the username of the Hudson user on 
the demo environment.
