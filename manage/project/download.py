from __future__ import with_statement
import urllib2
import time
import shutil
from os import stat, path as p
try: import simplejson as json
except ImportError: import json


scriptdir = p.dirname(__file__)
sourcefile = p.join(scriptdir, "download.json")
basedir = p.dirname(p.abspath(sourcefile))

def download(match=None, usemodtime=True):
    usemodtime = _unrepr(usemodtime)
    with open(sourcefile) as f:
        downloads = json.load(f)
    if match and match in downloads:
        groupname = match
        match = None
    else:
        groupname = None
    for name, group in downloads.items():
        if groupname and groupname != name:
            continue
        print "[Group: %s]" % name
        destdir = p.normpath(p.join(basedir, group.get('destination', "")))
        headers = (group.get('headers') or {}).items()
        named = group.get('named') or {}
        items = group.get('items') or ()
        resources = ( [(url, p.join(destdir, name))
                       for name, url in named.items()] +
                      [(url, destdir) for url in items] )
        for url, dest in resources:
            if match and match not in url:
                continue
            _http_get(url, dest, usemodtime=usemodtime, headers=headers)

_unrepr = lambda v: eval(v) if isinstance(v, str) else v

def _http_get(url, dest, usemodtime=True, headers=()):
    if p.isdir(dest):
        dest = p.join(dest, url.rsplit('/', 1)[-1])
    print "Downloading <%(url)s> to <%(dest)s> ..."%vars(),
    req = urllib2.Request(url)
    for header, value in headers:
        req.add_header(header, value)
    if p.exists(dest):
        if not usemodtime:
            print "Destination exists, skipping."
            return
        modstamp = time.strftime("%a, %d %b %Y %H:%M:%S GMT",
                time.gmtime(stat(dest).st_mtime))
        req.add_header('If-Modified-Since', modstamp)
    try:
        res = urllib2.urlopen(req)
    except Exception, e:
        print e
        return
    print res.info()
    print "Done."
    with file(dest, 'w') as out:
        shutil.copyfileobj(res, out)

