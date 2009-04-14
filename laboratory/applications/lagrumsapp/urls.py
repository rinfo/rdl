# coding=utf-8
from django.conf.urls.defaults import *

import os

# Slå på Djangos automatiska administrationssgränssnitt
from django.contrib import admin
admin.autodiscover()

# Konfigurera URL-routing

urlpatterns = patterns('',
    # Se till att filer i mappen static skickas
    (r'^static/(?P<path>.*)$', 'django.views.static.serve', {'document_root': os.path.join(os.path.dirname(__file__), 'static').replace('\\','/')}),

    # Startsidan ("/")
    (r'^$', 'lagrumsapp.rinfo.views.index'),

    # Enskild föreskrift (t.ex. "/publ/RA-FS/2006:6"
    (r'^publ/(?P<fskortnamn>.*)/(?P<fsnummer>.*)/$', 'lagrumsapp.rinfo.views.item'),

    # Slå på administrationsgränssnitt
    (r'^admin/(.*)', admin.site.root),
)
