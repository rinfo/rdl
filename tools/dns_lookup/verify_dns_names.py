import socket
import sys

lookup_name = {

    # beta
    '46.21.106.19': ['beta.lagrummet.se',
                     'www.beta.lagrummet.se'],
    '46.21.106.52': ['rinfo.beta.lagrummet.se'],
    '46.21.106.27': ['admin.beta.lagrummet.se'],
    '46.21.106.155': ['service.beta.lagrummet.se'],
    '46.21.106.120': ['checker.beta.lagrummet.se'],

    # demo
    '46.21.106.37': ['demo.lagrummet.se'],
    '46.21.106.39': ['rinfo.demo.lagrummet.se'],
    '46.21.106.38': ['admin.demo.lagrummet.se'],
    '46.21.106.43': ['service.demo.lagrummet.se'],
    '46.21.106.44': ['checker.demo.lagrummet.se'],

    # test
    '46.21.106.55': ['test.lagrummet.se',
                     '+.test.lagrummet.se'],

    # regression
    '46.21.106.35': ['regression.lagrummet.se',
                     '+.regression.lagrummet.se'],

    # testfeed
    '46.21.107.182': ['testfeed.lagrummet.se',
                      'regression.testfeed.lagrummet.se'],
    # skrapat (really old test feed but still in use)
    '79.99.1.133': ['skrapat.lagrummet.se',
                    '+.skrapat.lagrummet.se'],

    # Negative test. Lookop should fail
    '109.74.8.1': ['-not.lagrummet.se'],

    # FST
    '94.247.169.138': ['fst.lagrummet.se'],

    # produktion (currently reserved for use by virtual machines)
    '46.21.106.61': ['admin.lagrummet.se',
                     'rinfo.lagrummet.se'],
    '94.247.169.67': ['dev.lagrummet.se'],
    # produktion (currently NOT reserved for use by virtual machines)
    '83.145.60.248': ['www.lagrummet.se',
                      'lagrummet.se'],
    '94.247.169.66': ['checker.lagrummet.se',
                      'service.lagrummet.se'],

    # Doc
    '109.74.5.72': ['dokumentation.lagrummet.se'],
    }

rinfo_names = ('rinfo', 'admin', 'service', 'checker')

any_names = ('rinfo', 'admin', 'www', 'ds', 'service', 'checker', 'a')

success = True
count = 0


def check_dns_name(host_name, ip, fail=False):
    global count
    global success
    count += 1
    looked_up_ip = ''
    try:
        looked_up_ip = socket.gethostbyname(host_name)
    except:
        pass
    if fail:
        if looked_up_ip == ip:
            print('Lookup failed for "%s". Should not be %s!' % (host_name, ip) )
            success = False
        return
    if looked_up_ip != ip:
        if looked_up_ip=='':
            print('Lookup failed for "%s". Should be %s, but was not found!' % (host_name, ip) )
        else:
            print('Lookup failed for "%s". Should be %s, but was %s!' % (host_name, ip, looked_up_ip) )
        success = False


for ip in lookup_name.keys():
    for host_name in lookup_name[ip]:
        if host_name.startswith('*.'):
            # print('Checking any names for %s' % host_name )
            for any_name in any_names:
                check_dns_name(any_name + host_name[1:], ip)
        elif host_name.startswith('+.'):
            # print('Checking rinfo names for %s' % host_name )
            for rinfo_name in rinfo_names:
                check_dns_name(rinfo_name + host_name[1:], ip)
        elif host_name.startswith('-'):
            check_dns_name(host_name[1:], ip, fail=True)
        else:
            check_dns_name(host_name, ip)

if success:
    print('Successfully tested %s dns entries' % count)
else:
    print('Not all DNS lookups where successfull!')
    sys.exit(1)
