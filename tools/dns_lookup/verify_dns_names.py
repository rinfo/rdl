import socket
import sys

lookup_name = {'94.247.169.67': ['demo.lagrummet.se',
                                                  '+.demo.lagrummet.se'],
               '109.74.8.123':  ['test.lagrummet.se',
                                                  '+.test.lagrummet.se'],
               # beta
               '46.21.106.19':  ['beta.lagrummet.se',
                                                  'www.beta.lagrummet.se'],
               '46.21.106.52':  ['rinfo.beta.lagrummet.se'],
               '46.21.106.27':  ['admin.beta.lagrummet.se'],
               '46.21.106.155':  ['service.beta.lagrummet.se'],
               '46.21.106.120':  ['checker.beta.lagrummet.se'],

               # regression
               '46.21.106.35':  ['regression.lagrummet.se',
                                                  '+.regression.lagrummet.se'],

               # Negative test. Lookop should fail
               '109.74.8.1':    ['-not.lagrummet.se'],
              }

rinfo_names = ('www', 'rinfo', 'admin', 'service', 'checker')

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
        #e = sys.exc_info()[0]
        #print ('Failed to lookup %s ip %s because %s' % (host_name, ip ,e ) )
        #if not fail:
        #    success = False
    if fail:
        if looked_up_ip == ip:
            print('Lookup failed for "%s". Should not be %s' % (host_name, ip) )
            success = False
        #else:
        #    print('Lookup ok for "%s". Was %s' % (host_name, ip) )
        return

    if looked_up_ip != ip:
        print('Lookup failed for "%s". Should be %s, but was %s' % (host_name, ip, looked_up_ip) )
        success = False
    #else:
    #    print('Lookup ok for "%s". Was %s' % (host_name, ip) )

for ip in lookup_name.keys():
    for host_name in lookup_name[ip]:
        if host_name.startswith('*.'):
            #print('Checking any names for %s' % host_name )
            for any_name in any_names:
                check_dns_name(any_name+host_name[1:], ip)
        elif host_name.startswith('+.'):
            #print('Checking rinfo names for %s' % host_name )
            for rinfo_name in rinfo_names:
                check_dns_name(rinfo_name+host_name[1:], ip)
        elif host_name.startswith('-'):
            check_dns_name(host_name[1:], ip, fail=True)
        else:
            check_dns_name(host_name, ip)

if success:
    print('Successfully tested %s dns entries' % count)
else:
    print('Not all DNS lookups where successfull!')
    sys.exit(1)