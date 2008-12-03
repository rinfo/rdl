
def install_base():
    local("cd $(java_packages)/rinfo-base/; mvn install")

def install_store():
    local("cd $(java_packages)/rinfo-store/; mvn install")

def install_collector():
    local("cd $(java_packages)/rinfo-collector/; mvn install")

