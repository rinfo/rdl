"""
Specific tasks spanning over multiple apps
"""
from fabric.api import *
from fabfile.app.main import destroy_main_data
from fabfile.app.service import destroy_service_data
from fabfile.server import tomcat_stop, tomcat_start


@task
@roles('main','service')
def destroy_all_data(start_top_tomcat=True):
    if start_top_tomcat:
        tomcat_stop()
    destroy_main_data(start_top_tomcat=False)
    destroy_service_data(start_top_tomcat=False)
    if start_top_tomcat:
        tomcat_start()