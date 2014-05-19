import urllib
import sys
import feedparser
import os
import errno
import socket

# set over all time out to 1 sec to prevent stalling
socket.setdefaulttimeout(1)

if len(sys.argv) != 3:
    print('Feed parser and downloader v1.0')
    print('Usage: feed.py [base url] [relative start atom feed url]')
    quit()

base_url = sys.argv[1]
parse_url = base_url+sys.argv[2]

list_of_urls_to_download_structure=list()

def add_to_list(rel_url):
    if rel_url.startswith('/'):
	rel_url = rel_url[1:]
    if rel_url.startswith(base_url):
        list_of_urls_to_download_structure.append(rel_url)
    else:    
        list_of_urls_to_download_structure.append(base_url+rel_url)

def make_sure_directory_exists(file_name_and_path):
    try:
        os.makedirs(os.path.abspath(os.path.dirname(file_name_and_path)))
    except OSError, exc:
        if exc.errno != errno.EEXIST:
            raise
    return file_name_and_path

# Go throu atom feed and store all links
while parse_url:
    print ('Processing '+parse_url)
    add_to_list(parse_url)
    d = feedparser.parse(parse_url)
    for entry in d.entries: 
        try:
	    if entry.content[0].src:
                add_to_list(entry.content[0].src)
        except AttributeError:
	    pass
        try:
            for link in entry.links:
                if link.rel in ['alternate','enclosure']:
                    add_to_list(link.href)
        except AttributeError:
	    pass
    parse_url = ''
    try:
        for link in d.feed.links:
            if link.rel == 'prev-archive':
                parse_url = base_url+link.href
    except AttributeError:
        pass

# Download all links in to usable structure
print ('Found %i item(s)' % len(list_of_urls_to_download_structure) )
for item in list_of_urls_to_download_structure:
    print ('Downloading '+item)
    try:
        urllib.urlretrieve(item, make_sure_directory_exists(item[len(base_url):]))
    except IOError:
        print ('IOError: failed to download %s' % item)

