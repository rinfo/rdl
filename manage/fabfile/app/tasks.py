"""
Specific tasks spanning over multiple apps
"""
from fabric.api import *
from fabfile.app.main import destroy_main_data
from fabfile.app.service import destroy_service_data
from fabfile.server import tomcat_stop, tomcat_start


@task
@roles('service','main')
def destroy_all_data(start_stop_tomcat=True):
    destroy_service_data()
    destroy_main_data(start_stop_tomcat=start_stop_tomcat)
