from fabric.api import *

@task
@role('ci')
def install():
    sudo("apt-get install git")
    sudo("apt-get install maven2") # TODO: maven3
    sudo("apt-get install groovy") # TODO: groovy-1.8, or gradle...
    configure_groovy_grapes()
    sudo("apt-get install python-dev")
    sudo("apt-get install python-pip")
    sudo("pip install fabric")

def configure_groovy_grapes():
    run("mkdir -p ~/.groovy/")
    # TODO:
    #put("grapeConfig.xml", "~/.groovy/grapeConfig.xml")

