"""
Application Tools
"""
from fabric.api import *


def ping_collector(collector_url, feed_url):
    local("curl --data 'feed=%(feed_url)s' %(collector_url)s" % vars())

@task
def ping_main_collector(feed_url):
    #require('roledefs', provided_by=target)
    collector_url = "http://%s/collector" % env.roledefs['main'][0]
    #feed_url = "http://%s:8182/feed/current" % env.roledefs['examples'][0]
    ping_collector(collector_url, feed_url)

@task
def ping_service_collector():
    #require('roledefs', provided_by=target)
    collector_url = "http://%s/collector" % env.roledefs['service'][0]
    feed_url = "http://%s/feed/current" % env.roledefs['main'][0]
    ping_collector(collector_url, feed_url)

