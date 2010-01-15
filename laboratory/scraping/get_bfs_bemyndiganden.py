# -*- coding: UTF-8 -*-
import re
import os
from fnmatch import fnmatch
from subprocess import Popen, PIPE

bemynd_pattern = re.compile(ur'[Mm]ed stöd av (.*?förordningen.*?) \((\S+:\S+)\)')

def find_bemyndigande(fpath):
    out = Popen(['pdftotext', fpath, '-'], stderr=PIPE, stdout=PIPE).stdout
    for l in out:
        for ref, sfsnr in bemynd_pattern.findall(l):
            ref = ref.strip()
            return ref, sfsnr

def find_pdfs(fdir, pattern= "*.pdf"):
    for root, dirs, fnames in os.walk(fdir):
        for fname in fnames:
            if fnmatch(fname, pattern):
                yield os.path.join(root, fname)

from sys import argv
fdir = argv[1]

for fpath in find_pdfs(fdir):
    bemynd = find_bemyndigande(fpath)
    print "<%s>:" % fpath,
    if bemynd:
        ref, sfsnr = bemynd
        print "[%(ref)s] SFS %(sfsnr)s"%vars()
    else:
        print "N/A"

