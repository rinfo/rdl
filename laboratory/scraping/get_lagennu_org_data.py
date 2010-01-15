from lxml.etree import parse
from urllib import quote

def slug(text):
    return quote(text.lower().replace(' ', '_'))

orgs = []
series = []
curr_org = None

# doc from: /usr/bin/curl -sk "https://lagen.nu/1976:725" > /tmp/sfs-1976_725.xhtml
doc = parse("/tmp/sfs-1976_725.xhtml")

for tr in doc.xpath("//h:table[preceding-sibling::h:h2[@id='B1R2']]/h:tr",
        namespaces={'h':"http://www.w3.org/1999/xhtml"}):
    items = [ td.text.strip().encode('utf-8') for td in tr.findall('*') ]
    i = len(items)
    if i == 1:
        name = items[0]
        uri = '<http://rinfo.lagrummet.se/org/%s>' % slug(name)
        orgs.append((name, uri))
        curr_org = uri
    elif i >= 2:
        if i == 2:
            full, short = items
            comment = None
        else:
            full, short, comment = items
        uri = '<http://rinfo.lagrummet.se/serie/fs/%s>' % slug(short)
        series.append((uri, full, short, comment, curr_org))
    else:
        print "#", "|".join(items)

for name, uri in orgs:
    print uri
    print '    a foaf:Organization;'
    print '    rdfs:label "%s"@sv .' % name
    print

for uri, full, short, comment, org_uri in series:
    print uri
    print '    a :Forfattningssamling;'
    print '    rdfs:label "%s"@sv;' % short
    print '    rdfs:comment "%s"@sv;' % full
    if comment:
        print '    rdfs:comment "%s"@sv;' % comment
    print '    dct:publisher %s .' % org_uri
    print

