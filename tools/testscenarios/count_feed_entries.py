import os
from lxml import etree

from sys import argv, exit
if len(argv) < 2:
    print "Usage: %s DEPOT_DIR" % argv[0]
    exit()
depot_dir = argv[1]

find_feeds = "find %(depot_dir)s/feed -name '*.atom'" % vars()
feed_paths = (l.strip() for l in os.popen(find_feeds))
entry_groups = (etree.parse(fpath).getroot().findall('{http://www.w3.org/2005/Atom}entry') for fpath in feed_paths)

entry_count = sum(len(entries) for entries in entry_groups)
file_count = int(''.join(l.strip() for l in os.popen("find %(depot_dir)s/publ -name '*.rdf' | wc -l" % vars())))
print "Number of entries in feed files:", entry_count
print "Number of RDF files in depot:", file_count
print "Ok (equal)!" if entry_count == file_count else "Not ok (not equal)!"

