
@depends(service)
def setup_service():
    run("mkdir $(dist_dir)", fail='ignore')
    sudo("mkdir $(rinfo_dir)", fail='ignore')
