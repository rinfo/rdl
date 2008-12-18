
# TODO:? replace these with just:
#def install_rinfo_pkg():
#    local("cd $(java_packages)/; mvn install")
# (This also "installs" final war:s etc.. Use mvn-param for install dest.?)

def install_base():
    local("cd $(java_packages)/rinfo-base/; mvn install")

def install_store():
    local("cd $(java_packages)/rinfo-store/; mvn install")

def install_collector():
    local("cd $(java_packages)/rinfo-collector/; mvn install")

